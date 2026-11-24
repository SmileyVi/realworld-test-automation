package io.realworld.qa.tests.api.articles;

import io.realworld.qa.api.client.ArticlesClient;
import io.realworld.qa.api.model.articles.request.ArticleDraft;
import io.realworld.qa.api.model.articles.request.ArticleUpdate;
import io.realworld.qa.api.model.articles.response.Article;
import io.realworld.qa.support.ConduitTest;
import io.realworld.qa.support.Scenario.Account;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.realworld.qa.api.ApiUtils.readArticleFromResponse;
import static io.realworld.qa.api.ApiUtils.readErrorsFromResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("api")
class ArticlesApiTests extends ConduitTest {
    private final ArticlesClient articlesClient = conduitApi.articles();

    @Test
    @DisplayName("An author can create, read, update, and delete an article")
    void articleLifecycle() {
        Account articleAuthor = scenario.articleAuthor();
        ArticleDraft articleDraft = scenario.generateUniqueArticleDraft();
        Response creationResponse = articlesClient.createNewArticle(articleAuthor.accessToken(), articleDraft);
        assertEquals(HttpStatus.SC_CREATED, creationResponse.statusCode(),
                "POST /articles should create the new article");

        Article createdArticle = readArticleFromResponse(creationResponse);
        assertNotNull(createdArticle, "Article creation response should contain an article object");
        String articleSlug = createdArticle.getSlug();
        assertThat(articleSlug).as("The created article should have a slug for later requests").isNotBlank();
        scenario.deleteArticleAfterTest(articleAuthor, articleSlug);
        assertAll("Created article content",
                () -> assertEquals(articleDraft.getTitle(), createdArticle.getTitle(),
                        "The created article title should match the draft"),
                () -> assertEquals(articleDraft.getBody(), createdArticle.getBody(),
                        "The created article body should match the draft"));

        scenario.waitForArticleAuthor(articleAuthor, articleSlug);
        Response publishedArticleResponse = articlesClient.getArticleBySlug(articleSlug);
        assertEquals(HttpStatus.SC_OK, publishedArticleResponse.statusCode(),
                () -> "GET should find the newly created article " + articleSlug);
        Article publishedArticle = readArticleFromResponse(publishedArticleResponse);
        assertNotNull(publishedArticle, () -> "Response for " + articleSlug + " should contain an article object");
        assertEquals(articleDraft.getDescription(), publishedArticle.getDescription(),
                "The published description should match the draft");

        ArticleUpdate articleUpdate = new ArticleUpdate("Updated description", "Updated article content.");
        Response updateResponse = articlesClient.updateArticle(articleAuthor.accessToken(), articleSlug, articleUpdate);
        assertEquals(HttpStatus.SC_OK, updateResponse.statusCode(),
                () -> "The author should be able to update article " + articleSlug);
        Article updatedArticle = readArticleFromResponse(updateResponse);
        assertNotNull(updatedArticle, "Article update response should contain the updated article");
        assertAll("Updated article",
                () -> assertEquals(articleSlug, updatedArticle.getSlug(),
                        "Updating the description and body should preserve the article slug"),
                () -> assertEquals(articleUpdate.getBody(), updatedArticle.getBody(),
                        "The update response should contain the submitted article body"));

        Response persistedArticleResponse = articlesClient.getArticleBySlug(articleSlug);
        assertEquals(HttpStatus.SC_OK, persistedArticleResponse.statusCode(),
                () -> "GET should find article " + articleSlug + " after its update");
        Article persistedArticle = readArticleFromResponse(persistedArticleResponse);
        assertNotNull(persistedArticle,
                () -> "The response after updating " + articleSlug + " should contain an article object");
        assertEquals(articleUpdate.getDescription(), persistedArticle.getDescription(),
                "The updated description should persist after a new GET");

        Response deletionResponse = articlesClient.deleteArticle(articleAuthor.accessToken(), articleSlug);
        assertEquals(HttpStatus.SC_OK, deletionResponse.statusCode(),
                () -> "The author should be able to delete article " + articleSlug);
        Response deletedArticleResponse = articlesClient.getArticleBySlug(articleSlug);
        assertEquals(HttpStatus.SC_NOT_FOUND, deletedArticleResponse.statusCode(),
                () -> "GET should return not found after deleting " + articleSlug);
        assertThat(readErrorsFromResponse(deletedArticleResponse))
                .as("GET for deleted article %s should explain the error", articleSlug)
                .hasEntrySatisfying("body", errorMessages -> assertThat(errorMessages)
                        .as("The response for the deleted article should contain non-blank error messages")
                        .isNotEmpty().allMatch(errorMessage -> errorMessage != null && !errorMessage.isBlank()));
    }

    @Test
    @DisplayName("Only the author can delete an article")
    void rejectDeletionByAnotherUser() {
        Account articleAuthor = scenario.articleAuthor();
        Account otherAccount = scenario.registerTestAccount();
        Article publishedArticle = scenario.createArticleFixture(articleAuthor, scenario.generateUniqueArticleDraft());
        String articleSlug = publishedArticle.getSlug();

        Response forbiddenDeletionResponse = articlesClient.deleteArticle(otherAccount.accessToken(), articleSlug);
        assertEquals(HttpStatus.SC_FORBIDDEN, forbiddenDeletionResponse.statusCode(),
                () -> "Only the author should be able to delete article " + articleSlug);
        assertThat(readErrorsFromResponse(forbiddenDeletionResponse))
                .as("The forbidden deletion response should explain the error")
                .hasEntrySatisfying("body", errorMessages -> assertThat(errorMessages)
                        .as("The response should contain non-blank messages about the forbidden deletion")
                        .isNotEmpty().allMatch(errorMessage -> errorMessage != null && !errorMessage.isBlank()));

        Response articleAfterDeletionAttemptResponse = articlesClient.getArticleBySlug(articleSlug);
        assertEquals(HttpStatus.SC_OK, articleAfterDeletionAttemptResponse.statusCode(),
                () -> "Article " + articleSlug + " should still be accessible after the forbidden deletion attempt");
        Article articleAfterDeletionAttempt = readArticleFromResponse(articleAfterDeletionAttemptResponse);
        assertNotNull(articleAfterDeletionAttempt, () -> "The response should still contain article " + articleSlug);
        assertEquals(publishedArticle.getTitle(), articleAfterDeletionAttempt.getTitle(),
                "The forbidden deletion attempt should leave the title intact");
    }
}
