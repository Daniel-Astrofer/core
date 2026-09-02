package com.kerosene.auth.integration;

import com.kerosene.common.security.workload.InternalServiceRestTemplateFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

abstract class KfeRemoteClientSupport {

    private static final String DEFAULT_BASE_URL = "http://kfe-service:8080";
    protected final RestTemplate restTemplate;
    protected final String baseUrl;

    protected KfeRemoteClientSupport(
            InternalServiceRestTemplateFactory restTemplateFactory,
            String baseUrl,
            long connectTimeoutMs,
            long readTimeoutMs) {
        InternalServiceRestTemplateFactory.ConfiguredClient client = restTemplateFactory.create(
                baseUrl,
                DEFAULT_BASE_URL,
                connectTimeoutMs,
                readTimeoutMs);
        this.restTemplate = client.restTemplate();
        this.baseUrl = client.baseUrl();
    }

    protected <T> HttpEntity<T> internalJsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    protected HttpEntity<Void> internalEntity() {
        return new HttpEntity<>(new HttpHeaders());
    }
}
