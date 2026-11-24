package io.realworld.qa.config;

import org.apache.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.awaitility.Awaitility.await;

public final class ConduitEnvironment {
    private ConduitEnvironment() {}

    public static void awaitApi(ApiConfig apiConfig) {
        ApiConfig.Readiness readiness = apiConfig.readiness();
        awaitHttp(apiConfig.readinessUrl(), readiness.connectTimeout(), readiness.requestTimeout(),
                readiness.waitTimeout(), readiness.pollInterval());
    }

    public static void awaitWeb(TestConfig config) {
        awaitHttp(config.webUrl(), Duration.ofSeconds(2), Duration.ofSeconds(3),
                Duration.ofSeconds(30), Duration.ofMillis(500));
    }

    private static void awaitHttp(URI uri, Duration connectTimeout, Duration requestTimeout, Duration waitTimeout,
                                  Duration pollInterval) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout).build()) {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(requestTimeout).GET().build();
            await("Conduit is ready at " + uri)
                    .atMost(waitTimeout)
                    .pollInterval(pollInterval)
                    .ignoreException(IOException.class)
                    .until(() -> client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
                            == HttpStatus.SC_OK);
        }
    }
}
