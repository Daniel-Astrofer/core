package com.kerosene.auth.integration;

import org.junit.jupiter.api.Test;
import com.kerosene.common.security.workload.InternalServiceRestTemplateFactory;
import com.kerosene.common.security.workload.WorkloadIdentityProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KfeRemoteFinancialWalletProvisioningClientTest {

    @Test
    void postsPrimaryWalletProvisioningRequestToKfe() throws Exception {
        KfeRemoteFinancialWalletProvisioningClient client = new KfeRemoteFinancialWalletProvisioningClient(
                legacyClientFactory("credential"),
                "http://kfe.test",
                100,
                100);
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate(client));
        server.expect(requestTo("http://kfe.test/internal/kfe/wallet-provisioning/primary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"userId\":42,\"initialAddress\":\"bc1qabc\"}"))
                .andRespond(withSuccess());

        client.ensurePrimaryWalletReady(42L, "bc1qabc");

        server.verify();
    }

    @Test
    void postsPrimaryWalletRepairRequestWithoutInitialAddress() throws Exception {
        KfeRemoteFinancialWalletProvisioningClient client = new KfeRemoteFinancialWalletProvisioningClient(
                legacyClientFactory("credential"),
                "http://kfe.test",
                100,
                100);
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate(client));
        server.expect(requestTo("http://kfe.test/internal/kfe/wallet-provisioning/primary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"userId\":42,\"initialAddress\":null}"))
                .andRespond(withSuccess());

        client.ensurePrimaryWalletReady(42L, null);

        server.verify();
    }

    @Test
    void rejectsMissingInternalCredentialBeforeCallingKfe() {
        KfeRemoteFinancialWalletProvisioningClient client = new KfeRemoteFinancialWalletProvisioningClient(
                legacyClientFactory(""),
                "http://kfe.test",
                100,
                100);

        assertThrows(IllegalStateException.class, () -> client.ensurePrimaryWalletReady(42L, null));
    }

    private RestTemplate restTemplate(KfeRemoteFinancialWalletProvisioningClient client) throws Exception {
        Field field = KfeRemoteClientSupport.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        return (RestTemplate) field.get(client);
    }

    private InternalServiceRestTemplateFactory legacyClientFactory(String secret) {
        return new InternalServiceRestTemplateFactory(
                new WorkloadIdentityProperties().toConfig(), null, secret);
    }
}
