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

class KfeRemoteFinancialAuditIntegrityClientTest {

    @Test
    void fetchesAuditRootFromKfe() throws Exception {
        KfeRemoteFinancialAuditIntegrityClient client = new KfeRemoteFinancialAuditIntegrityClient(
                new InternalServiceRestTemplateFactory(
                        new WorkloadIdentityProperties().toConfig(), null, "credential"),
                "http://kfe.test",
                100,
                100);
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate(client));
        server.expect(requestTo("http://kfe.test/internal/kfe/audit-integrity/root"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"rootVersion\":1,\"hashAlgorithm\":\"SHA-256\",\"merkleRoot\":\"abc123\","
                                + "\"eventCount\":7,\"fromSequence\":1,\"toSequence\":7,\"previousRoot\":null,"
                                + "\"generatedAt\":\"2026-06-24T10:15:30Z\",\"signerKeyId\":\"test-signer\","
                                + "\"signature\":\"test-signature\",\"checkpointId\":\"checkpoint-7\"}",
                        MediaType.APPLICATION_JSON));

        var root = client.currentRoot();

        assertEquals("abc123", root.merkleRoot());
        assertEquals(7L, root.eventCount());
        assertEquals(1L, root.fromSequence());
        assertEquals(7L, root.toSequence());
        server.verify();
    }

    private RestTemplate restTemplate(KfeRemoteFinancialAuditIntegrityClient client) throws Exception {
        Field field = KfeRemoteClientSupport.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        return (RestTemplate) field.get(client);
    }
}
