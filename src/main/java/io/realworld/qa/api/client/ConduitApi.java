package io.realworld.qa.api.client;

import io.realworld.qa.config.ApiConfig;

public final class ConduitApi {
    private final ArticlesClient articles;
    private final UsersClient users;
    private final UserClient user;

    public ConduitApi(ApiConfig apiConfig) {
        ConduitRequests requests = new ConduitRequests(apiConfig.baseUrl(), apiConfig.transport());
        articles = new ArticlesClient(requests, apiConfig.routes().articles());
        users = new UsersClient(requests, apiConfig.routes().users());
        user = new UserClient(requests, apiConfig.routes().currentUser());
    }

    public ArticlesClient articles() {
        return articles;
    }

    public UsersClient users() {
        return users;
    }

    public UserClient user() {
        return user;
    }
}
