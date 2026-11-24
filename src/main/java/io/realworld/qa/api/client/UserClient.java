package io.realworld.qa.api.client;

import io.restassured.response.Response;

public final class UserClient {
    private final ConduitRequests requests;
    private final String currentUserPath;

    UserClient(ConduitRequests requests, String currentUserPath) {
        this.requests = requests;
        this.currentUserPath = currentUserPath;
    }

    public Response getCurrentUser(String accessToken) {
        return requests.newAuthenticatedRequest(accessToken).get(currentUserPath);
    }
}
