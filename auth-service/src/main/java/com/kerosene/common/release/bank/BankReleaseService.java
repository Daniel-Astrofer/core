package com.kerosene.common.release.bank;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kerosene.common.release.ReleaseCanonicalJson;
import com.kerosene.common.release.ReleaseEvidenceFiles;
import com.kerosene.common.release.ReleaseManifestService;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.Semaphore;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** A signed runtime read, not release authorization or an independent rebuild. */
@Service
public class BankReleaseService {
    private static final ObjectReader JSON = com.kerosene.config.ReleaseJsonConfig.reader();
    private final BankReleaseProperties config;
    private final ReleaseManifestService runtime;
    private final Environment environment;
    private final Clock clock;
    private final Semaphore requests = new Semaphore(4);
    private PrivateKey key;
    private Path targets;

    @org.springframework.beans.factory.annotation.Autowired
    public BankReleaseService(BankReleaseProperties config, ReleaseManifestService runtime, Environment environment) {
        this(config, runtime, environment, Clock.systemUTC());
    }
    BankReleaseService(BankReleaseProperties config, ReleaseManifestService runtime, Environment environment, Clock clock) {
        this.config = config; this.runtime = runtime; this.environment = environment; this.clock = clock;
    }
    @PostConstruct public void initialize() {
        if (!config.enabled) return;
        try {
            if (!Boolean.parseBoolean(environment.getProperty("server.ssl.enabled", "false"))
                    || !"need".equalsIgnoreCase(environment.getProperty("server.ssl.client-auth", "")))
                throw new IllegalArgumentException("direct mandatory server mTLS required");
            if (!config.observerId.matches("[a-z0-9][a-z0-9._-]{2,127}")
                    || !config.networkId.matches("[a-z0-9][a-z0-9._-]{2,127}")
                    || config.clientSpkiDigests.isEmpty() || config.clientSpkiDigests.size() > 64
                    || config.clientSpkiDigests.stream().distinct().count() != config.clientSpkiDigests.size()
                    || !config.clientSpkiDigests.stream().allMatch(p -> p.matches("sha256:[0-9a-f]{64}")))
                throw new IllegalArgumentException("invalid Bank identity or client pins");
            targets = ReleaseEvidenceFiles.directory(config.targetDirectory);
            byte[] encoded = ReleaseEvidenceFiles.read(Path.of(config.privateKeyFile), 8192, true);
            try { key = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(encoded)); }
            finally { Arrays.fill(encoded, (byte) 0); }
            var publicKey = KeyFactory.getInstance("Ed25519").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(config.publicKeyDerBase64)));
            if (!config.publicKeyDerBase64.equals(Base64.getEncoder().encodeToString(publicKey.getEncoded())))
                throw new IllegalArgumentException("canonical Bank SPKI encoding required");
            byte[] probe = "kerosene.bank-key-pair-check/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var signing = Signature.getInstance("Ed25519"); signing.initSign(key); signing.update(probe);
            var verifying = Signature.getInstance("Ed25519"); verifying.initVerify(publicKey); verifying.update(probe);
            if (!verifying.verify(signing.sign())) throw new IllegalArgumentException("Bank key pair mismatch");
        } catch (Exception failure) {
            key = null;
            throw new IllegalStateException("Bank release observer configuration is unsafe or unreadable");
        }
    }
    public ObjectNode observe(String digest, String challenge) {
        if (!config.enabled || key == null) throw error(HttpStatus.SERVICE_UNAVAILABLE, "observer_unconfigured");
        if (digest == null || !digest.matches("sha256:[0-9a-f]{64}")
                || challenge == null || !challenge.matches("[0-9a-f]{64}")) throw error(HttpStatus.BAD_REQUEST, "query_invalid");
        if (!requests.tryAcquire()) throw error(HttpStatus.TOO_MANY_REQUESTS, "observer_busy");
        try {
            ObjectNode target = target(digest);
            var snapshot = runtime.snapshot();
            // The present runtime can prove a signed mismatch, not new-release compatibility.
            String status = snapshot.manifestSignatureValid() && !snapshot.authorized() ? "incompatible" : "unknown";
            Instant now = clock.instant();
            ObjectNode read = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("schema", "kerosene.bank-release-read/v1")
                    .put("releaseId", target.path("releaseId").asText()).put("networkId", config.networkId)
                    .put("targetSequence", target.path("sequence").longValue()).put("releaseLockCanonicalDigest", digest)
                    .put("issuedAt", now.toString()).put("expiresAt", now.plusSeconds(30).toString())
                    .put("challenge", challenge).put("source", "bank-runtime");
            read.putObject("observation").put("observerId", config.observerId).put("status", status)
                    .put("observedSequence", 0).put("releaseDigest", digest).put("observedAt", now.toString());
            var signing = Signature.getInstance("Ed25519"); signing.initSign(key); signing.update(ReleaseCanonicalJson.bytes(read));
            read.putArray("signatures").addObject().put("memberId", config.observerId)
                    .put("publicKeyDerBase64", config.publicKeyDerBase64)
                    .put("signatureBase64", Base64.getEncoder().encodeToString(signing.sign()));
            return read;
        } catch (ResponseStatusException failure) { throw failure; }
        catch (Exception failure) { throw error(HttpStatus.SERVICE_UNAVAILABLE, "bank_evidence_unavailable"); }
        finally { requests.release(); }
    }
    private ObjectNode target(String digest) throws Exception {
        // This operator-staged catalog identifies targets; it grants no approval.
        Path path = targets.resolve(digest.substring(7) + ".json");
        byte[] bytes;
        try { bytes = ReleaseEvidenceFiles.read(path, 262144, false); }
        catch (java.nio.file.NoSuchFileException absent) { throw error(HttpStatus.NOT_FOUND, "target_unknown"); }
        var parsed = JSON.readTree(bytes);
        if (!(parsed instanceof ObjectNode target)) throw new IllegalArgumentException("target object required");
        long version = target.path("schemaVersion").asLong(-1);
        if (!target.path("schemaVersion").isIntegralNumber() || version < 1 || version > 3
                || !("kerosene.release-lock/v" + version).equals(target.path("schema").asText())
                || !target.path("releaseId").asText().matches("[a-z0-9][a-z0-9._-]{2,127}")
                || !config.networkId.equals(target.path("network").path("id").asText())
                || !"bank".equals(target.path("network").path("plane").asText())
                || !target.path("sequence").isIntegralNumber() || !target.path("sequence").canConvertToLong()
                || target.path("sequence").longValue() < 1 || target.path("sequence").longValue() > 9007199254740991L)
            throw new IllegalArgumentException("invalid target domain");
        String actual = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ReleaseCanonicalJson.bytes(target)));
        if (!digest.equals(actual)) throw new IllegalArgumentException("target digest mismatch");
        return target;
    }
    static ResponseStatusException error(HttpStatus status, String code) { return new ResponseStatusException(status, code); }
}
