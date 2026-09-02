package com.kerosene.common.security.workload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalServiceAuthenticationFilterTest {

    private static final String KFE_ID = "spiffe://staging.kerosene.internal/service/kfe";

    @Test
    void legacyModeRequiresExplicitConstantTimeCredential() throws Exception {
        WorkloadIdentityProperties properties = new WorkloadIdentityProperties();
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                properties.toConfig(), "credential", List.of("/internal/kfe/"));
        MockHttpServletRequest request = request("/internal/kfe/test");
        request.addHeader(InternalServiceRestTemplateFactory.LEGACY_HEADER, "credential");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void legacyModeFailsClosedWithoutConfiguredCredential() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                new WorkloadIdentityProperties().toConfig(), "", List.of("/internal/kfe/"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/internal/kfe/test"), response, new MockFilterChain());

        assertEquals(503, response.getStatus());
    }

    @Test
    void exactInternalRouteIsProtected() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                new WorkloadIdentityProperties().toConfig(), "credential", List.of("/internal/kfe"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/internal/kfe"), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void mtlsModeRejectsProtectedPathOnPublicConnector() throws Exception {
        WorkloadIdentityProperties properties = mtlsProperties();
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                properties.toConfig(), "ignored", List.of("/internal/kfe/"));
        MockHttpServletRequest request = request("/internal/kfe/test");
        request.addHeader(InternalServiceRestTemplateFactory.LEGACY_HEADER, "ignored");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void mtlsModeAcceptsOnlyExactPeerUriSanOnInternalConnector() throws Exception {
        WorkloadIdentityProperties properties = mtlsProperties();
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                properties.toConfig(), "", List.of("/internal/kfe/"));
        MockHttpServletRequest request = request("/internal/kfe/test");
        request.setSecure(true);
        request.setLocalPort(8443);
        request.setAttribute(
                InternalServiceAuthenticationFilter.CLIENT_CERTIFICATE_ATTRIBUTE,
                new X509Certificate[]{certificateWithSans(List.of(List.of(6, KFE_ID)))});
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void mtlsModeRejectsDifferentOrAmbiguousUriSan() throws Exception {
        WorkloadIdentityProperties properties = mtlsProperties();
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                properties.toConfig(), "", List.of("/internal/kfe/"));
        MockHttpServletRequest request = request("/internal/kfe/test");
        request.setSecure(true);
        request.setLocalPort(8443);
        request.setAttribute(
                InternalServiceAuthenticationFilter.CLIENT_CERTIFICATE_ATTRIBUTE,
                new X509Certificate[]{certificateWithSans(List.of(
                        List.of(6, KFE_ID),
                        List.of(6, "spiffe://staging.kerosene.internal/service/node")))});
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void unprotectedPublicPathDoesNotRequireServiceIdentity() throws Exception {
        InternalServiceAuthenticationFilter filter = new InternalServiceAuthenticationFilter(
                mtlsProperties().toConfig(), "", List.of("/internal/kfe/"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/health/live"), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    private static WorkloadIdentityProperties mtlsProperties() {
        WorkloadIdentityProperties properties = new WorkloadIdentityProperties();
        properties.setEnabled(true);
        properties.setPeerSpiffeId(KFE_ID);
        properties.setInternalPort(8443);
        return properties;
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("POST", path);
    }

    @SuppressWarnings("unchecked")
    private static X509Certificate certificateWithSans(List<List<?>> sans) throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectAlternativeNames()).thenReturn((List) sans);
        return certificate;
    }
}
