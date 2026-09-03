package com.kerosene.config.production;

public class TextPropertyProductionSafetyCheck extends AbstractProductionSafetyCheck {

    public TextPropertyProductionSafetyCheck(ProductionSafetyCheck next) {
        super(next);
    }

    @Override
    protected void inspect(ProductionSafetyContext context) {
        String bitcoinNetwork = context.environment().getProperty("bitcoin.network", "");
        if (!"testnet3".equalsIgnoreCase(bitcoinNetwork) && !"testnet".equalsIgnoreCase(bitcoinNetwork)) {
            context.addViolation("bitcoin.network must be testnet3");
        }
        String corsOrigins = context.environment().getProperty("app.cors.allowed-origins", "");
        if (corsOrigins.isBlank()) {
            context.addViolation("app.cors.allowed-origins must be configured");
        }
        if (corsOrigins.contains("*")) {
            context.addViolation("wildcard CORS is not allowed");
        }
        if (corsOrigins.contains("localhost") || corsOrigins.contains("127.0.0.1")) {
            context.addViolation("localhost CORS origins are not allowed in prod");
        }

        String relyingPartyId = context.environment().getProperty("webauthn.relying-party-id", "");
        if (relyingPartyId.isBlank() || "localhost".equalsIgnoreCase(relyingPartyId)) {
            context.addViolation("webauthn.relying-party-id must be a production host");
        }

        boolean meshOnly = context.environment().getProperty("kfe.vaultmesh.mesh-only", Boolean.class, false);
        boolean meshEnabled = context.environment().getProperty("kfe.vaultmesh.enabled", Boolean.class, false);

        if (meshOnly || meshEnabled) {
            if (context.environment().getProperty("kfe.vaultmesh.base-url", "").isBlank()) {
                context.addViolation("kfe.vaultmesh.base-url must be configured");
            }
            if (context.environment().getProperty("kfe.vaultmesh.require-mtls", Boolean.class, false)) {
                String apiToken = context.environment().getProperty("kfe.vaultmesh.api-token", "");
                if (apiToken != null && !apiToken.isBlank()) {
                    context.addViolation(
                            "kfe.vaultmesh.api-token must be empty when require-mtls"
                                    + " (static_token refused in go-live)");
                }
                String cert = context.environment().getProperty("kfe.vaultmesh.tls.cert-path", "");
                String key = context.environment().getProperty("kfe.vaultmesh.tls.key-path", "");
                String ca = context.environment().getProperty("kfe.vaultmesh.tls.ca-path", "");
                String keystore = context.environment().getProperty("kfe.vaultmesh.tls.keystore-path", "");
                String truststore = context.environment().getProperty("kfe.vaultmesh.tls.truststore-path", "");
                boolean pem = notBlank(cert) && notBlank(key) && notBlank(ca);
                boolean pkcs12 = notBlank(keystore) && notBlank(truststore);
                if (!pem && !pkcs12) {
                    context.addViolation(
                            "kfe.vaultmesh.tls PEM or PKCS12 materials must be configured when require-mtls");
                }
            }
        } else {
            String quorumPeers = context.environment().getProperty("quorum.shard.urls", "");
            if (quorumPeers.isBlank()) {
                context.addViolation("quorum.shard.urls must define remote shard peers");
            }
        }

        java.util.List<String> required = new java.util.ArrayList<>(java.util.List.of(
                "bitcoin.platform.master-xpub",
                "shard.attestation.secret"));
        if (!(meshOnly || meshEnabled)) {
            required.add("quorum.psbt.signer-urls");
            required.add("quorum.psbt.signer-ids");
        }
        for (String propertyName : required) {
            if (context.environment().getProperty(propertyName, "").isBlank()) {
                context.addViolation(propertyName + " must be configured");
            }
        }

        if (context.environment().getProperty("lightning.lnd.enabled", Boolean.class, false)) {
            for (String propertyName : java.util.List.of("lightning.lnd.host", "lightning.lnd.tls.cert-path")) {
                if (context.environment().getProperty(propertyName, "").isBlank()) {
                    context.addViolation(propertyName + " must be configured");
                }
            }
            String macaroon = context.environment().getProperty("lightning.lnd.macaroon", "");
            String macaroonPath = context.environment().getProperty("lightning.lnd.macaroon-path", "");
            if (macaroon.isBlank() && macaroonPath.isBlank()) {
                context.addViolation("lightning.lnd.macaroon or lightning.lnd.macaroon-path must be configured");
            }
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
