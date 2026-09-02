package com.kerosene.auth.integration;

import org.junit.jupiter.api.Test;
import com.kerosene.common.security.workload.InternalServiceRestTemplateFactory;
import com.kerosene.common.security.workload.WorkloadIdentityProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KfeRemoteFinancialRailHealthClientTest {

    @Test
    void fetchesCustodyProviderHealthFromKfe() throws Exception {
        KfeRemoteFinancialRailHealthClient client = client();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate(client));
        server.expect(requestTo("http://kfe.test/internal/kfe/rail-health/custody-provider"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"providerName\":\"BITCOIN_CORE\",\"live\":true,\"implementation\":\"Adapter\"}",
                        MediaType.APPLICATION_JSON));

        var status = client.custodyProvider();

        assertEquals("BITCOIN_CORE", status.providerName());
        assertEquals("Adapter", status.implementation());
        server.verify();
    }

    @Test
    void fetchesExternalRailProvidersFromKfe() throws Exception {
        KfeRemoteFinancialRailHealthClient client = client();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate(client));
        server.expect(requestTo("http://kfe.test/internal/kfe/rail-health/external-providers"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"onchain\":{\"providerName\":\"BITCOIN_CORE\",\"live\":true,\"implementation\":\"Onchain\"}}",
                        MediaType.APPLICATION_JSON));

        var providers = client.activeRailProviders();

        assertEquals("BITCOIN_CORE", providers.get("onchain").providerName());
        server.verify();
    }

    private KfeRemoteFinancialRailHealthClient client() {
        return new KfeRemoteFinancialRailHealthClient(
                new InternalServiceRestTemplateFactory(
                        new WorkloadIdentityProperties().toConfig(), null, "credential"),
                "http://kfe.test",
                100,
                100);
    }

    private RestTemplate restTemplate(KfeRemoteFinancialRailHealthClient client) throws Exception {
        Field field = KfeRemoteClientSupport.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        return (RestTemplate) field.get(client);
    }
}
