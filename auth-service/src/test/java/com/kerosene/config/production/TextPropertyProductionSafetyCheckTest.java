package com.kerosene.config.production;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class TextPropertyProductionSafetyCheckTest {

    @Test
    void rejectsSwappedOrUnrelatedServiceRoles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "kerosene.workload-identity.own-spiffe-id",
                        "spiffe://prod.kerosene.internal/service/kfe")
                .withProperty(
                        "kerosene.workload-identity.peer-spiffe-id",
                        "spiffe://prod.kerosene.internal/service/auth")
                .withProperty("kerosene.workload-identity.socket", "unix:///run/spire.sock")
                .withProperty("kfe.internal.base-url", "https://kfe-service:8443")
                .withProperty("kfe.remote.base-url", "https://kfe-service:8443");
        ProductionSafetyContext context = new ProductionSafetyContext(
                environment, new DefaultListableBeanFactory());

        new TextPropertyProductionSafetyCheck(null).handle(context);

        assertThat(context.violations())
                .contains(
                        "kerosene.workload-identity.own-spiffe-id must end with /service/auth",
                        "kerosene.workload-identity.peer-spiffe-id must end with /service/kfe");
    }
}
