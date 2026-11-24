package io.realworld.qa.config;

import io.restassured.mapper.ObjectMapperType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;

import static io.realworld.qa.config.ConfigValues.httpBaseUri;
import static io.realworld.qa.config.ConfigValues.setting;

public record ApiConfig(URI baseUrl, Routes routes, Transport transport, Readiness readiness,
                        Duration articleAuthorWait) {
    private static final String RESOURCE = "/api.properties";
    private static final String API_URL_KEY = "conduit.apiUrl";

    public static ApiConfig load() {
        Properties file = loadProperties();
        URI baseUrl = httpBaseUri(API_URL_KEY, setting(API_URL_KEY, "CONDUIT_API_URL", fileValue(file, API_URL_KEY)));
        Routes routes = new Routes(
                new ArticleRoutes(path(file, "api.articles.path.collection"), path(file, "api.articles.path.bySlug")),
                new UsersRoutes(path(file, "api.users.path.registration"), path(file, "api.users.path.login")),
                path(file, "api.user.path.current"));
        Transport transport = new Transport(
                value(file, "api.http.auth.header"), value(file, "api.http.auth.scheme"),
                value(file, "api.http.mediaType"), mapperType(file),
                timeout(file, "api.http.connectTimeout"), timeout(file, "api.http.socketTimeout"));
        Readiness readiness = new Readiness(
                path(file, "api.readiness.path"),
                duration(file, "api.readiness.connectTimeout.millis"),
                duration(file, "api.readiness.requestTimeout.millis"),
                duration(file, "api.readiness.waitTimeout.millis"),
                duration(file, "api.readiness.pollInterval.millis"));
        return new ApiConfig(baseUrl, routes, transport, readiness, duration(file, "api.articles.authorWait.millis"));
    }

    public URI readinessUrl() {
        return URI.create(baseUrl + readiness.path());
    }

    private static Properties loadProperties() {
        try (InputStream propertiesStream = ApiConfig.class.getResourceAsStream(RESOURCE)) {
            if (propertiesStream == null) {
                throw new IllegalStateException("Missing API configuration resource " + RESOURCE);
            }
            Properties file = new Properties();
            file.load(propertiesStream);
            return file;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read API configuration " + RESOURCE, exception);
        }
    }

    private static String fileValue(Properties file, String key) {
        return requiredValue(key, file.getProperty(key));
    }

    private static String value(Properties file, String key) {
        return requiredValue(key, System.getProperty(key, file.getProperty(key)));
    }

    private static String requiredValue(String key, String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            throw new IllegalArgumentException("Missing API configuration value for " + key);
        }
        return configuredValue.strip();
    }

    private static String path(Properties file, String key) {
        String configuredPath = value(file, key);
        if (!configuredPath.startsWith("/") || configuredPath.startsWith("//")
                || configuredPath.contains("?") || configuredPath.contains("#")) {
            throw new IllegalArgumentException(key + " must be a path relative to the API base URL");
        }
        return configuredPath;
    }

    private static ObjectMapperType mapperType(Properties file) {
        String key = "api.http.mapper";
        String mapper = value(file, key);
        try {
            return ObjectMapperType.valueOf(mapper);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(key + " has an unknown REST Assured mapper: " + mapper, exception);
        }
    }

    private static Timeout timeout(Properties file, String keyPrefix) {
        return new Timeout(value(file, keyPrefix + ".parameter"), positiveMillis(file, keyPrefix + ".millis"));
    }

    private static Duration duration(Properties file, String key) {
        return Duration.ofMillis(positiveMillis(file, key));
    }

    private static int positiveMillis(Properties file, String key) {
        int millis;
        try {
            millis = Integer.parseInt(value(file, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a positive integer in milliseconds", exception);
        }
        if (millis <= 0) {
            throw new IllegalArgumentException(key + " must be a positive integer in milliseconds");
        }
        return millis;
    }

    public record Routes(ArticleRoutes articles, UsersRoutes users, String currentUser) {}

    public record ArticleRoutes(String collection, String bySlug) {}

    public record UsersRoutes(String registration, String login) {}

    public record Transport(String authHeader, String authScheme, String mediaType, ObjectMapperType mapperType,
                            Timeout connectTimeout, Timeout socketTimeout) {}

    public record Timeout(String parameter, int millis) {}

    public record Readiness(String path, Duration connectTimeout, Duration requestTimeout, Duration waitTimeout,
                            Duration pollInterval) {}
}
