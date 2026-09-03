package com.kerosene.config.production;

import java.util.List;

public class BooleanPropertyProductionSafetyCheck extends AbstractProductionSafetyCheck {

    private static final List<String> PROHIBITED_TRUE_FLAGS = List.of(
            "bitcoin.mock-mode",
            "custody.mock-mode",
            "quorum.allow-local-simulation",
            "treasury.siphon.manual-settlement-enabled",
            "transactions.onchain.test-instant-settlement-enabled");

    public BooleanPropertyProductionSafetyCheck(ProductionSafetyCheck next) {
        super(next);
    }

    @Override
    protected void inspect(ProductionSafetyContext context) {
        for (String flag : PROHIBITED_TRUE_FLAGS) {
            if (context.environment().getProperty(flag, Boolean.class, false)) {
                context.addViolation(flag + "=true");
            }
        }

        boolean meshOnly = context.environment().getProperty("kfe.vaultmesh.mesh-only", Boolean.class, false);
        boolean meshEnabled = context.environment().getProperty("kfe.vaultmesh.enabled", Boolean.class, false);

        if (meshOnly || meshEnabled) {
            requireTrue(context, "kfe.vaultmesh.enabled", false);
            if (meshOnly) {
                if (context.environment().getProperty("kfe.mpc.signing-enabled", Boolean.class, true)) {
                    context.addViolation("kfe.mpc.signing-enabled must be false under mesh-only");
                }
            }
            // Mesh owns custody governance — HashiCorp bootstrap / mpc-sidecar removed.
            // Go-live / staging profile: require-mtls refuses lab static_token.
            if (context.environment().getProperty("kfe.vaultmesh.require-mtls", Boolean.class, false)) {
                requireTrue(context, "kfe.vaultmesh.tls.enabled", false);
            }
        }

        if (context.environment().getProperty("lightning.lnd.enabled", Boolean.class, false)) {
            context.addViolation("lightning.lnd.enabled must be false until Lightning custody is threshold-distributed");
        }
        requireTrue(context, "bitcoin.rpc.enabled", false);
        requireTrue(context, "bitcoin.rpc.required", false);
        requireTrue(context, "bitcoin.rpc.pruned-required", false);
        requireTrue(context, "tor.health.required", false);
        requireTrue(context, "release.attestation.required", false);
        requireTrue(context, "release.attestation.remote.enabled", false);
        if (!(meshOnly || meshEnabled)) {
            requireTrue(context, "quorum.psbt.require-signer-identity", true);
        }
    }

    private void requireTrue(ProductionSafetyContext context, String propertyName, boolean defaultValue) {
        if (!context.environment().getProperty(propertyName, Boolean.class, defaultValue)) {
            context.addViolation(propertyName + " must be true");
        }
    }
}
