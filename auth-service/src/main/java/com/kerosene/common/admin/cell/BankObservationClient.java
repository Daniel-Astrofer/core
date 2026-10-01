package com.kerosene.common.admin.cell;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.stereotype.Component;

/** Fixed operator-configured origin; caller input never chooses an upstream. */
@Component
public class BankObservationClient {
    static final int MAX_BYTES = 1_048_576;
    private final CellOperationsProperties config;
    public BankObservationClient(CellOperationsProperties config) { this.config = config; }

    public byte[] fetch() throws Exception {
        if (!config.targetReleaseDigest.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalStateException("BANK_TARGET_DIGEST_NOT_CONFIGURED");
        }
        return fetch(config.bankUrl, "/v1/releases/observations?releaseDigest="
                + java.net.URLEncoder.encode(config.targetReleaseDigest, java.nio.charset.StandardCharsets.UTF_8), null);
    }

    byte[] fetch(String origin, String path, String token) throws Exception {
        URI base = URI.create(origin);
        if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null
                || !(base.getPath().isEmpty() || base.getPath().equals("/"))) {
            throw new IllegalStateException("BANK_HTTPS_ORIGIN_REQUIRED");
        }
        if (config.keyStore.isBlank() || config.trustStore.isBlank()) {
            throw new IllegalStateException("BANK_MTLS_NOT_CONFIGURED");
        }
        SSLContext tls = SSLContext.getInstance("TLSv1.3");
        char[] keyPassword = config.keyStorePassword.toCharArray();
        char[] trustPassword = config.trustStorePassword.toCharArray();
        try {
            KeyStore keys = load(config.keyStore, keyPassword);
            if (java.util.Collections.list(keys.aliases()).stream().noneMatch(alias -> {
                try { return keys.isKeyEntry(alias); } catch (Exception e) { return false; }
            })) throw new IllegalStateException("BANK_CLIENT_KEY_MISSING");
            var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(keys, keyPassword);
            var tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tm.init(load(config.trustStore, trustPassword));
            tls.init(km.getKeyManagers(), tm.getTrustManagers(), null);
        } finally {
            Arrays.fill(keyPassword, '\0');
            Arrays.fill(trustPassword, '\0');
        }
        var client = HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var builder = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        var request = builder.GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) throw new IllegalStateException("BANK_UNAVAILABLE");
            byte[] bytes = body.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IllegalStateException("BANK_RESPONSE_TOO_LARGE");
            return bytes;
        }
    }

    private KeyStore load(String file, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(Path.of(file))) { store.load(input, password); }
        return store;
    }
}
