package io.realworld.qa.config;

import java.net.URI;

final class ConfigValues {
    private ConfigValues() {}

    static URI httpBaseUri(String key, String value) {
        URI uri = URI.create(value.replaceAll("/+$", ""));
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(key + " must be an HTTP(S) base URL");
        }
        return uri;
    }

    static String setting(String property, String variable, String fallback) {
        String value = System.getProperty(property, System.getenv(variable));
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
