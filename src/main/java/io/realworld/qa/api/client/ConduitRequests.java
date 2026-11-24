package io.realworld.qa.api.client;

import io.qameta.allure.Allure;
import io.realworld.qa.config.ApiConfig.Transport;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.config.RestAssuredConfig.config;

final class ConduitRequests {
    private final URI baseUrl;
    private final Transport transport;
    private final RestAssuredConfig restAssuredConfig;

    ConduitRequests(URI baseUrl, Transport transport) {
        this.baseUrl = baseUrl;
        this.transport = transport;
        this.restAssuredConfig = config()
                .objectMapperConfig(objectMapperConfig().defaultObjectMapperType(transport.mapperType()))
                .httpClient(httpClientConfig()
                        .setParam(transport.connectTimeout().parameter(), transport.connectTimeout().millis())
                        .setParam(transport.socketTimeout().parameter(), transport.socketTimeout().millis()));
    }

    RequestSpecification newAuthenticatedRequest(String accessToken) {
        return newJsonRequest().header(transport.authHeader(), transport.authScheme() + " " + accessToken);
    }

    RequestSpecification newJsonRequest() {
        return given().baseUri(baseUrl.getScheme() + "://" + baseUrl.getRawAuthority())
                .basePath(baseUrl.getPath())
                .config(restAssuredConfig)
                .contentType(transport.mediaType())
                .accept(transport.mediaType())
                .filter((requestSpecification, responseSpecification, context) -> {
                    Response httpResponse = context.next(requestSpecification, responseSpecification);
                    String operation = requestSpecification.getMethod() + " " + requestSpecification.getUserDefinedPath();
                    Allure.addAttachment("HTTP " + operation, "text/plain",
                            httpResponse.statusLine() + "\nDuration: " + httpResponse.time() + " ms");
                    return httpResponse;
                });
    }
}
