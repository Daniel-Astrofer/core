package com.kerosene.common.release.bank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kerosene.common.release.ReleaseCanonicalJson;
import com.kerosene.common.release.ReleaseManifestService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real container TLS and the production certificate-only chain, not header simulation. */
class BankReleaseMtlsTest {
    @TempDir static Path temporary;
    static final char[] PASSWORD = "disposable-test-only".toCharArray();
    static final ObjectMapper JSON = new ObjectMapper();
    static ServletWebServerApplicationContext application;
    static KeyPair signingKey;
    static KeyStore server, client, other;
    static HttpClient authorized, unpinned, anonymous;
    static String digest, origin;
    static ObjectNode releaseLock;

    @Configuration @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"})
    @EnableConfigurationProperties(BankReleaseProperties.class)
    @Import({BankReleaseSecurity.class, BankReleaseController.class, BankReleaseService.class})
    static class TestApplication {
        @Bean ReleaseManifestService runtime() {
            var runtime = mock(ReleaseManifestService.class);
            when(runtime.snapshot()).thenReturn(new ReleaseManifestService.ReleaseSnapshot("core", "unknown", "unknown",
                    "unknown", "unknown", "unknown", "unknown", "absent", false, true,
                    "ATTESTATION_OPTIONAL", "Unconfigured test runtime", Map.of(), Map.of()));
            return runtime;
        }
    }
    static KeyStore identity(String name, String usage) throws Exception {
        Path file = temporary.resolve(name + ".p12");
        var command = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", name, "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12",
                "-keystore", file.toString(), "-storepass", String.valueOf(PASSWORD), "-keypass", String.valueOf(PASSWORD),
                "-dname", "CN=" + name, "-validity", "2", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-ext", "EKU=" + usage);
        command.redirectErrorStream(true).redirectOutput(temporary.resolve(name + "-keytool.log").toFile());
        var process = command.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("test keytool timeout"); }
        assertEquals(0, process.exitValue(), "disposable certificate generation failed");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        var store = KeyStore.getInstance("PKCS12"); try (var stream = Files.newInputStream(file)) { store.load(stream, PASSWORD); }
        return store;
    }
    static HttpClient client(KeyStore identity) throws Exception {
        var trust = KeyStore.getInstance("PKCS12"); trust.load(null, PASSWORD);
        trust.setCertificateEntry("server", server.getCertificate("server"));
        var trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); trustFactory.init(trust);
        javax.net.ssl.KeyManager[] keys = null;
        if (identity != null) {
            var factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); factory.init(identity, PASSWORD); keys = factory.getKeyManagers();
        }
        var tls = SSLContext.getInstance("TLS"); tls.init(keys, trustFactory.getTrustManagers(), null);
        return HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }
    @BeforeAll static void start() throws Exception {
        server = identity("server", "serverAuth"); client = identity("node", "clientAuth"); other = identity("other", "clientAuth");
        var trust = KeyStore.getInstance("PKCS12"); trust.load(null, PASSWORD);
        // Both identities are CA-trusted; only one is allowed by the application pin.
        trust.setCertificateEntry("node", client.getCertificate("node")); trust.setCertificateEntry("other", other.getCertificate("other"));
        Path trustFile = temporary.resolve("server-trust.p12"); try (var out = Files.newOutputStream(trustFile)) { trust.store(out, PASSWORD); }
        signingKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Path keyFile = temporary.resolve("bank-key.der"); Files.write(keyFile, signingKey.getPrivate().getEncoded());
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
        Path targets = Files.createDirectory(temporary.resolve("targets"));
        Files.setPosixFilePermissions(targets, PosixFilePermissions.fromString("rwx------"));
        ObjectNode lock = JSON.createObjectNode().put("schema", "kerosene.release-lock/v3").put("schemaVersion", 3)
                .put("releaseId", "release-one").put("sequence", 2);
        lock.putObject("network").put("id", "bank-main").put("plane", "bank");
        releaseLock = lock;
        digest = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ReleaseCanonicalJson.bytes(lock)));
        Files.write(targets.resolve(digest.substring(7) + ".json"), JSON.writeValueAsBytes(lock));
        Files.setPosixFilePermissions(targets.resolve(digest.substring(7) + ".json"), PosixFilePermissions.fromString("rw-r--r--"));
        String pin = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(client.getCertificate("node").getPublicKey().getEncoded()));
        var properties = Map.ofEntries(Map.entry("server.port", "0"), Map.entry("server.address", "127.0.0.1"),
                        Map.entry("server.ssl.enabled", "true"), Map.entry("server.ssl.client-auth", "need"),
                        Map.entry("server.ssl.key-store", temporary.resolve("server.p12").toString()),
                        Map.entry("server.ssl.key-store-password", String.valueOf(PASSWORD)), Map.entry("server.ssl.key-store-type", "PKCS12"),
                        Map.entry("server.ssl.trust-store", trustFile.toString()), Map.entry("server.ssl.trust-store-password", String.valueOf(PASSWORD)),
                        Map.entry("server.ssl.trust-store-type", "PKCS12"), Map.entry("spring.main.banner-mode", "off"),
                        Map.entry("spring.security.user.password", "unused-disposable-test-only"), Map.entry("logging.level.root", "ERROR"),
                        Map.entry("release.bank-observer.enabled", "true"), Map.entry("release.bank-observer.network-id", "bank-main"),
                        Map.entry("release.bank-observer.observer-id", "bank-one"), Map.entry("release.bank-observer.target-directory", targets.toString()),
                        Map.entry("release.bank-observer.private-key-file", keyFile.toString()),
                        Map.entry("release.bank-observer.public-key-der-base64", Base64.getEncoder().encodeToString(signingKey.getPublic().getEncoded())),
                        Map.entry("release.bank-observer.client-spki-digests", pin));
        application = (ServletWebServerApplicationContext) new SpringApplicationBuilder(TestApplication.class)
                .run(properties.entrySet().stream().map(entry -> "--" + entry.getKey() + "=" + entry.getValue()).toArray(String[]::new));
        origin = "https://localhost:" + application.getWebServer().getPort();
        authorized = client(client); unpinned = client(other); anonymous = client(null);
    }
    @AfterAll static void stop() { if (application != null) application.close(); }
    static HttpRequest request(String query) {
        return HttpRequest.newBuilder(URI.create(origin + "/v1/releases/observation?" + query)).timeout(Duration.ofSeconds(5)).GET().build();
    }
    static String query() { return "releaseDigest=" + digest.replace(":", "%3A") + "&challenge=" + "a".repeat(64); }
    @Test void actualMtlsReadReturnsNormativeSignatureButNotCompatibility() throws Exception {
        var response = authorized.send(request(query()), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode()); assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        var read = (ObjectNode) JSON.readTree(response.body()); var signature = read.remove("signatures").get(0);
        var verifier = Signature.getInstance("Ed25519"); verifier.initVerify(signingKey.getPublic()); verifier.update(ReleaseCanonicalJson.bytes(read));
        assertTrue(verifier.verify(Base64.getDecoder().decode(signature.path("signatureBase64").asText())));
        assertEquals("unknown", read.path("observation").path("status").asText());
        assertEquals("a".repeat(64), read.path("challenge").asText());
    }
    @Test void missingCertificateCannotBeReplacedByAdminOrForwardedHeaders() {
        var request = HttpRequest.newBuilder(URI.create(origin + "/v1/releases/observation?" + query()))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer synthetic-admin-token")
                .header("X-Forwarded-Client-Cert", "CN=node").GET().build();
        assertThrows(java.io.IOException.class, () -> anonymous.send(request, HttpResponse.BodyHandlers.ofString()));
    }
    @Test void trustedButUnpinnedCertificateIsRejectedByProductionGate() throws Exception {
        var response = unpinned.send(request(query()), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode()); assertEquals("client_certificate_required", JSON.readTree(response.body()).path("code").asText());
    }
    @Test void duplicateCallerStatusAndPostCannotObtainSignedRead() throws Exception {
        for (String extra : new String[]{"&status=compatible", "&challenge=" + "b".repeat(64)}) {
            var response = authorized.send(request(query() + extra), HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode()); assertFalse(JSON.readTree(response.body()).has("signatures"));
        }
        var post = HttpRequest.newBuilder(URI.create(origin + "/v1/releases/observation?" + query())).timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(405, authorized.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void certificateAttributeOnCleartextRequestIsStillRejected() throws Exception {
        var properties = application.getBean(BankReleaseProperties.class);
        var request = new MockHttpServletRequest("GET", "/v1/releases/observation"); request.setQueryString(query()); request.setSecure(false);
        request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{(X509Certificate) client.getCertificate("node")});
        var response = new MockHttpServletResponse(); boolean[] called = {false};
        new BankReleaseSecurity.PeerGate(properties).doFilter(request, response, (r, s) -> called[0] = true);
        assertEquals(403, response.getStatus()); assertFalse(called[0]);
    }
    static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }
    @Test @org.junit.jupiter.api.Tag("node-core-wire")
    void productionRustNodeConsumesCoreAndRefusesUnknownForSigning() throws Exception {
        String probe = System.getenv("KEROSENE_NODE_BANK_PROBE");
        assertNotNull(probe, "explicit nodeCoreWireTest requires the compiled Node probe");
        Path executable = Path.of(probe);
        assertTrue(executable.isAbsolute() && Files.isRegularFile(executable) && Files.isExecutable(executable));
        Path ca = temporary.resolve("node-probe-ca.pem"), identity = temporary.resolve("node-probe-identity.pem");
        Files.writeString(ca, pem("CERTIFICATE", server.getCertificate("server").getEncoded()));
        Files.writeString(identity, pem("CERTIFICATE", client.getCertificate("node").getEncoded())
                + pem("PRIVATE KEY", client.getKey("node", PASSWORD).getEncoded()));
        Files.setPosixFilePermissions(identity, PosixFilePermissions.fromString("rw-------"));
        var configuration = JSON.createObjectNode().put("networkId", "bank-main")
                .put("identityPem", identity.toString()).put("caPem", ca.toString());
        configuration.set("releaseLock", releaseLock);
        configuration.putObject("bank").put("observerId", "bank-one").put("endpoint", origin)
                .put("publicKeyDerBase64", Base64.getEncoder().encodeToString(signingKey.getPublic().getEncoded()));
        Path config = temporary.resolve("node-probe.json"); Files.write(config, JSON.writeValueAsBytes(configuration));
        Files.setPosixFilePermissions(config, PosixFilePermissions.fromString("rw-------"));
        Path result = temporary.resolve("node-probe-result.log");
        var process = new ProcessBuilder(executable.toString(), config.toString()).redirectErrorStream(true).redirectOutput(result.toFile()).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("disposable Node wire probe timeout"); }
        String output = Files.readString(result);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("unknown target cannot authorize signing"), output);
    }
}
