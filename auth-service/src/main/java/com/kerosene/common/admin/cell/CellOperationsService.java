package com.kerosene.common.admin.cell;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kerosene.common.release.ReleaseManifestService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CellOperationsService {
    private final BankObservationClient bank;
    private final CellEvidenceVerifier verifier;
    private final ReleaseManifestService releases;
    private final CellOperationsProperties config;
    private final ObjectMapper mapper;
    private final KfeMaintenanceClient maintenance;
    public CellOperationsService(BankObservationClient bank, CellEvidenceVerifier verifier,
            ReleaseManifestService releases, CellOperationsProperties config, ObjectMapper mapper, KfeMaintenanceClient maintenance) {
        this.bank = bank; this.verifier = verifier; this.releases = releases; this.config = config; this.mapper = mapper;
        this.maintenance = maintenance;
    }

    public Map<String, Object> snapshot() {
        Instant now = Instant.now();
        Map<String, Object> result;
        try { result = verifier.verify(bank.fetch(), now); }
        catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            result = new LinkedHashMap<>();
            result.put("verification", "UNVERIFIED"); result.put("fresh", false); result.put("ready", false);
            result.put("currentRelease", Map.of()); result.put("targetRelease", Map.of());
            result.put("quorum", Map.of("requiredVotes", config.minimumVotes, "compatibleVotes", 0, "votes", List.of()));
            result.put("backups", List.of()); result.put("update", Map.of("phase", "UNKNOWN", "history", List.of()));
            result.put("blockers", List.of("BANK_EVIDENCE_UNAVAILABLE_OR_UNVERIFIED", "CURRENT_RELEASE_MISSING",
                    "TARGET_RELEASE_MISSING", "QUORUM_INSUFFICIENT", "RESTORE_EVIDENCE_MISSING", "UPDATE_HISTORY_MISSING"));
        }
        var runtime = releases.snapshot();
        var kfe = maintenance.status(now);
        result.put("kfeMaintenance", kfe);
        if (!Boolean.TRUE.equals(kfe.get("safeToUpdate"))) {
            var blockers = new ArrayList<>((List<String>) result.get("blockers"));
            blockers.add("KFE_MAINTENANCE_NOT_SAFE"); result.put("blockers", blockers); result.put("ready", false);
        }
        if (!runtime.manifestSignatureValid() || !runtime.authorized()) {
            var blockers = new ArrayList<>((List<String>) result.get("blockers"));
            blockers.add("CORE_RUNTIME_UNVERIFIED"); result.put("blockers", blockers); result.put("ready", false);
        }
        result.put("schema", "kerosene.cell-operations/v1"); result.put("checkedAt", now.toString());
        result.put("cellId", config.cellId); result.put("networkId", config.networkId); result.put("coreRuntime", runtime);
        result.put("maximumAgeSeconds", config.maximumAgeSeconds);
        result.put("capabilities", Map.of("plan", !config.planDirectory.isBlank(), "execute", false, "restore", false));
        return result;
    }

    /** Records intent and evidence only. This service never launches deployment or restore commands. */
    public synchronized Map<String, Object> plan(PlanRequest request, String actor, String requestId) throws Exception {
        if (request == null || request.targetReleaseId() == null
                || !request.targetReleaseId().matches("[a-z0-9][a-z0-9._-]{2,127}")
                || request.targetDigest() == null || !request.targetDigest().matches("sha256:[0-9a-f]{64}")
                || request.deploymentManifestDigest() == null || !request.deploymentManifestDigest().matches("sha256:[0-9a-f]{64}")
                || request.packageManifestDigest() == null || !request.packageManifestDigest().matches("sha256:[0-9a-f]{64}")
                || request.targetSequence() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid target identity required");
        Path directory = directory();
        var evidence = snapshot();
        Map<?, ?> target = (Map<?, ?>) evidence.get("targetRelease");
        if (!request.targetReleaseId().equals(target.get("releaseId"))
                || !request.targetDigest().equals(target.get("digest"))
                || !request.deploymentManifestDigest().equals(target.get("deploymentManifestDigest"))
                || !request.packageManifestDigest().equals(target.get("packageManifestDigest"))
                || !(target.get("sequence") instanceof Number sequence) || sequence.longValue() != request.targetSequence()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Target must match verified Bank evidence");
        }
        String id = UUID.randomUUID().toString();
        var plan = new LinkedHashMap<String, Object>();
        plan.put("schema", "kerosene.cell-update-plan/v1"); plan.put("planId", id);
        plan.put("createdAt", Instant.now().toString()); plan.put("actor", actor); plan.put("requestId", requestId);
        plan.put("cellId", config.cellId); plan.put("networkId", config.networkId);
        plan.put("targetReleaseId", request.targetReleaseId()); plan.put("targetDigest", request.targetDigest());
        plan.put("targetSequence", request.targetSequence()); plan.put("status", Boolean.TRUE.equals(evidence.get("ready")) ? "PLANNED" : "BLOCKED");
        plan.put("deploymentManifestDigest", request.deploymentManifestDigest()); plan.put("packageManifestDigest", request.packageManifestDigest());
        plan.put("deploymentExecuted", false); plan.put("evidenceDigest", evidence.get("evidenceDigest"));
        plan.put("blockers", evidence.get("blockers"));
        plan.put("kfeMaintenance", evidence.get("kfeMaintenance"));
        plan.put("steps", List.of("VERIFY_PACKAGE_OFFLINE", "CONFIRM_CURRENT_BACKUP_AND_RESTORE_EVIDENCE",
                "RECHECK_QUORUM_AND_TARGET", "HANDOFF_TO_DEPLOY_OWNER", "COLLECT_EXECUTION_AND_HEALTH_EVIDENCE"));
        Path temp = Files.createTempFile(directory, ".plan-", ".tmp");
        try {
            Files.write(temp, mapper.writeValueAsBytes(plan));
            try (var channel = java.nio.channels.FileChannel.open(temp, java.nio.file.StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, directory.resolve(id + ".json"), StandardCopyOption.ATOMIC_MOVE);
            try (var channel = java.nio.channels.FileChannel.open(directory, java.nio.file.StandardOpenOption.READ)) { channel.force(true); }
        } finally { Files.deleteIfExists(temp); }
        return plan;
    }

    public List<Map<String, Object>> plans() throws Exception {
        if (config.planDirectory.isBlank()) return List.of();
        try (var files = Files.list(directory())) {
            var paths = files.filter(p -> p.getFileName().toString().matches("[0-9a-f-]{36}\\.json"))
                    .sorted(Comparator.comparingLong(this::modified).reversed()).limit(100).toList();
            var result = new ArrayList<Map<String, Object>>();
            for (Path path : paths) result.add(read(path));
            return result;
        }
    }
    public Map<String, Object> plan(String id) throws Exception {
        if (!id.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Path file = directory().resolve(id + ".json");
        if (!Files.exists(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return read(file);
    }
    private Map<String, Object> read(Path file) throws Exception {
        rejectLinks(file);
        var attributes = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.size() > BankObservationClient.MAX_BYTES) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        try (var input = Files.newInputStream(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(BankObservationClient.MAX_BYTES + 1);
            if (bytes.length > BankObservationClient.MAX_BYTES) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            return mapper.readValue(bytes, Map.class);
        }
    }
    private long modified(Path file) {
        try { return Files.getLastModifiedTime(file).toMillis(); } catch (Exception e) { return Long.MIN_VALUE; }
    }
    private Path directory() throws Exception {
        if (config.planDirectory.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plan storage not configured");
        Path directory = Path.of(config.planDirectory).toAbsolutePath().normalize();
        if (directory.getParent() == null || Files.isSymbolicLink(directory)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        rejectLinks(directory);
        Files.createDirectories(directory, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
        rejectLinks(directory);
        if (Files.getPosixFilePermissions(directory).stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plan directory must be private");
        }
        return directory;
    }
    private void rejectLinks(Path path) throws Exception {
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Links are forbidden in plan storage");
        }
    }
    public record PlanRequest(String targetReleaseId, long targetSequence, String targetDigest,
            String deploymentManifestDigest, String packageManifestDigest) {}
}
