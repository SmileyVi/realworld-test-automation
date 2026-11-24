package io.realworld.qa.api.client;

import io.realworld.qa.api.model.users.request.Credentials;
import io.realworld.qa.api.model.users.request.Registration;
import io.realworld.qa.api.model.users.request.UserRequest;
import io.realworld.qa.config.ApiConfig.UsersRoutes;
import io.restassured.response.Response;

public final class UsersClient {
    private final ConduitRequests requests;
    private final UsersRoutes routes;

    UsersClient(ConduitRequests requests, UsersRoutes routes) {
        this.requests = requests;
        this.routes = routes;
    }

    public Response registerUser(Registration registration) {
        return requests.newJsonRequest().body(new UserRequest<>(registration)).post(routes.registration());
    }

    public Response logInUser(Credentials credentials) {
        return requests.newJsonRequest().body(new UserRequest<>(credentials)).post(routes.login());
    }
}
