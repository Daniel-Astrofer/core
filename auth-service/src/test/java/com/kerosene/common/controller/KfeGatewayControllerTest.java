package com.kerosene.common.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KfeGatewayControllerTest {

    private RestTemplate restTemplate;
    private KfeGatewayController controller;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        controller = new KfeGatewayController(restTemplate, "http://kfe-service:8080");
    }

    @Test
    void timeoutReturns504GatewayTimeout() {
        SocketTimeoutException timeout = new SocketTimeoutException("Read timed out");
        ResourceAccessException rae = new ResourceAccessException("I/O error", timeout);
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(rae);

        var request = new MockHttpServletRequest("GET", "/kfe/wallets/test");
        var response = controller.proxy(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).contains("SYS_504");
    }

    @Test
    void otherExceptionsReturn502BadGateway() {
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Something broke"));

        var request = new MockHttpServletRequest("POST", "/kfe/transactions");
        var response = controller.proxy(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains("SYS_502");
    }

    @Test
    void normalProxyReturnsUpstreamResponse() {
        var upstreamResponse = new org.springframework.http.ResponseEntity<>(
                "{\"success\":true}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(upstreamResponse);

        var request = new MockHttpServletRequest("GET", "/kfe/wallets/test");
        var response = controller.proxy(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("success");
    }

    @Test
    void proxiesPublicAndAdminNamespacesWithoutChangingTheirPaths() {
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("{}"));

        controller.proxy(new MockHttpServletRequest("GET", "/api/public/kfe/payment-requests/id"));
        controller.proxy(new MockHttpServletRequest("GET", "/api/admin/kfe/audit/latest"));

        ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(2))
                .exchange(urls.capture(), any(HttpMethod.class), any(), eq(String.class));
        assertThat(urls.getAllValues()).containsExactly(
                "http://kfe-service:8080/api/public/kfe/payment-requests/id",
                "http://kfe-service:8080/api/admin/kfe/audit/latest");
    }

    @Test
    void translatesPublicKfeReadinessToTheInternalHealthEndpoint() {
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok("{}"));

        controller.proxy(new MockHttpServletRequest("GET", "/kfe/health/ready"));

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(restTemplate)
                .exchange(url.capture(), eq(HttpMethod.GET), any(), eq(String.class));
        assertThat(url.getValue()).isEqualTo("http://kfe-service:8080/health/ready");
    }
}
