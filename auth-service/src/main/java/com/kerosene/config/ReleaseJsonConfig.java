package com.kerosene.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Isolated protocol codec; immutable readers/writers cannot change global business JSON. */
@Configuration(proxyBeanMethods = false)
public class ReleaseJsonConfig {
    private static final ObjectMapper PROTOCOL = new ObjectMapper(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public static ObjectReader reader() { return PROTOCOL.reader(); }
    public static ObjectWriter writer() { return PROTOCOL.writer(); }
    @Bean("releaseProtocolReader") public ObjectReader releaseProtocolReader() { return reader(); }
    @Bean("releaseProtocolWriter") public ObjectWriter releaseProtocolWriter() { return writer(); }
}
