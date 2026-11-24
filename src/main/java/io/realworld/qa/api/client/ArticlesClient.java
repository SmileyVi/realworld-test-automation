package io.realworld.qa.api.client;

import io.realworld.qa.api.model.articles.request.ArticleDraft;
import io.realworld.qa.api.model.articles.request.ArticleRequest;
import io.realworld.qa.api.model.articles.request.ArticleUpdate;
import io.realworld.qa.config.ApiConfig.ArticleRoutes;
import io.restassured.response.Response;

public final class ArticlesClient {
    private final ConduitRequests requests;
    private final ArticleRoutes routes;

    ArticlesClient(ConduitRequests requests, ArticleRoutes routes) {
        this.requests = requests;
        this.routes = routes;
    }

    public Response createNewArticle(String accessToken, ArticleDraft articleDraft) {
        return requests.newAuthenticatedRequest(accessToken)
                .body(new ArticleRequest<>(articleDraft))
                .post(routes.collection());
    }

    public Response getArticleBySlug(String articleSlug) {
        return requests.newJsonRequest().get(routes.bySlug(), articleSlug);
    }

    public Response updateArticle(String accessToken, String articleSlug, ArticleUpdate articleUpdate) {
        return requests.newAuthenticatedRequest(accessToken)
                .body(new ArticleRequest<>(articleUpdate))
                .put(routes.bySlug(), articleSlug);
    }

    public Response deleteArticle(String accessToken, String articleSlug) {
        return requests.newAuthenticatedRequest(accessToken).delete(routes.bySlug(), articleSlug);
    }
}
