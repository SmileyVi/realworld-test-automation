package io.realworld.qa.api;

import io.realworld.qa.api.model.articles.response.Article;
import io.realworld.qa.api.model.articles.response.ArticleResponse;
import io.realworld.qa.api.model.common.ErrorResponse;
import io.realworld.qa.api.model.common.User;
import io.realworld.qa.api.model.user.response.CurrentUserResponse;
import io.realworld.qa.api.model.users.response.UserResponse;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

public final class ApiUtils {
    private ApiUtils() {}

    public static User readUserFromResponse(Response response) {
        return response.as(UserResponse.class).getUser();
    }

    public static User readCurrentUserFromResponse(Response response) {
        return response.as(CurrentUserResponse.class).getUser();
    }

    public static Article readArticleFromResponse(Response response) {
        return response.as(ArticleResponse.class).getArticle();
    }

    public static Map<String, List<String>> readErrorsFromResponse(Response response) {
        return response.as(ErrorResponse.class).getErrors();
    }
}
