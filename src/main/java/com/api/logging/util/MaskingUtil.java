package com.api.logging.util;

import java.util.Set;

public class MaskingUtil {
    private static final String MASK = "********";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "otp", "token", "authorization", "cvv", "pan", "aadhaar", "creditcard"
    );

    public static String mask(String key, String value) {
        if (value == null) return null;
        if (SENSITIVE_FIELDS.contains(key.toLowerCase())) {
            return MASK;
        }
        return value;
    }

    public static String maskBody(String body) {
        if (body == null || body.isEmpty()) return body;
        String maskedBody = body;
        for (String field : SENSITIVE_FIELDS) {
            maskedBody = maskedBody.replaceAll("(?i)(\"" + field + "\"\\s*:\\s*\")([^\"]+)(\")", "$1" + MASK + "$3");
        }
        return maskedBody;
    }
}
