package com.kerosene.common.release;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.TreeMap;

/** Contracts release/v1 canonical bytes; no floating point or lossy integers. */
public final class ReleaseCanonicalJson {
    private ReleaseCanonicalJson() {}

    public static byte[] bytes(JsonNode node) throws java.io.IOException {
        return com.kerosene.config.ReleaseJsonConfig.writer().writeValueAsBytes(sorted(node));
    }

    private static Object sorted(JsonNode node) {
        if (node.isObject()) {
            var result = new TreeMap<String, Object>();
            node.properties().forEach(field -> result.put(field.getKey(), sorted(field.getValue())));
            return result;
        }
        if (node.isArray()) {
            var result = new ArrayList<Object>(); node.forEach(value -> result.add(sorted(value))); return result;
        }
        if (node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isIntegralNumber() && node.canConvertToLong()
                && node.longValue() >= -9007199254740991L && node.longValue() <= 9007199254740991L) return node.longValue();
        throw new IllegalArgumentException("noninteroperable canonical JSON number");
    }
}
