package io.realworld.qa.support;

import io.realworld.qa.api.client.ArticlesClient;
import io.realworld.qa.api.client.UsersClient;
import io.realworld.qa.api.model.articles.request.ArticleDraft;
import io.realworld.qa.api.model.articles.response.Article;
import io.realworld.qa.api.model.common.User;
import io.realworld.qa.api.model.users.request.Credentials;
import io.realworld.qa.api.model.users.request.Registration;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.realworld.qa.api.ApiUtils.readArticleFromResponse;
import static io.realworld.qa.api.ApiUtils.readUserFromResponse;
import static org.awaitility.Awaitility.await;

public final class Scenario implements AutoCloseable {
    public static final String TEST_USER_ALIAS = "testUser";

    private final UsersClient usersClient;
    private final ArticlesClient articlesClient;
    private final Duration articleAuthorWait;
    private final TestUsers testUsers;
    private final Deque<Runnable> pendingCleanupActions = new ArrayDeque<>();

    Scenario(UsersClient usersClient, ArticlesClient articlesClient, Duration articleAuthorWait, TestUsers testUsers) {
        this.usersClient = usersClient;
        this.articlesClient = articlesClient;
        this.articleAuthorWait = articleAuthorWait;
        this.testUsers = testUsers;
    }

    public Registration generateUniqueRegistration() {
        String username = "qa" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new Registration(username, username + "@example.test", "Qa-" + UUID.randomUUID());
    }

    public Account registerTestAccount() {
        Registration registration = generateUniqueRegistration();
        Response registrationResponse = usersClient.registerUser(registration);
        String operation = "Register account " + registration.getUsername();
        requireResponseStatus(registrationResponse, HttpStatus.SC_CREATED, operation);
        Credentials credentials = new Credentials(registration.getEmail(), registration.getPassword());
        return readFixtureAccount(registrationResponse, credentials, operation);
    }

    public Account articleAuthor() {
        return findConfiguredTestUser().orElseGet(this::registerTestAccount);
    }

    public Account authenticateTestAccount() {
        return findConfiguredTestUser().orElseGet(() ->
                logInFixtureAccount(registerTestAccount().credentials(), "Log in as generated test user"));
    }

    public Account authenticateConfiguredUser(String alias) {
        return logInConfiguredUser(alias, testUsers.requireCredentials(alias));
    }

    private Optional<Account> findConfiguredTestUser() {
        return testUsers.findCredentials(TEST_USER_ALIAS)
                .map(credentials -> logInConfiguredUser(TEST_USER_ALIAS, credentials));
    }

    private Account logInConfiguredUser(String alias, Credentials credentials) {
        return logInFixtureAccount(credentials, "Log in as test user '" + alias + "'");
    }

    private Account logInFixtureAccount(Credentials credentials, String operation) {
        Response loginResponse = usersClient.logInUser(credentials);
        requireResponseStatus(loginResponse, HttpStatus.SC_OK, operation);
        return readFixtureAccount(loginResponse, credentials, operation);
    }

    public ArticleDraft generateUniqueArticleDraft() {
        return generateUniqueArticleDraft(new ArticleDraft("QA article", "QA article description",
                "Article content created by the automation suite.", List.of()));
    }

    public ArticleDraft generateUniqueArticleDraft(ArticleDraft template) {
        return new ArticleDraft(template.getTitle() + " " + UUID.randomUUID(),
                template.getDescription(), template.getBody(), template.getTagList());
    }

    public Article createArticleFixture(Account articleAuthor, ArticleDraft articleDraft) {
        Response creationResponse = articlesClient.createNewArticle(articleAuthor.accessToken(), articleDraft);
        requireResponseStatus(creationResponse, HttpStatus.SC_CREATED, "Create article fixture " + articleDraft.getTitle());
        Article createdArticle = readArticleFromResponse(creationResponse);
        if (createdArticle == null || createdArticle.getSlug() == null || createdArticle.getSlug().isBlank()) {
            throw new IllegalStateException("Creation returned no slug for article " + articleDraft.getTitle());
        }
        deleteArticleAfterTest(articleAuthor, createdArticle.getSlug());
        waitForArticleAuthor(articleAuthor, createdArticle.getSlug());
        return createdArticle;
    }

    public void deleteArticleAfterTest(Account articleAuthor, String articleSlug) {
        pendingCleanupActions.push(() -> {
            Response deletionResponse = articlesClient.deleteArticle(articleAuthor.accessToken(), articleSlug);
            int deletionStatus = deletionResponse.statusCode();
            if (deletionStatus != HttpStatus.SC_OK && deletionStatus != HttpStatus.SC_NO_CONTENT
                    && deletionStatus != HttpStatus.SC_NOT_FOUND) {
                throw new IllegalStateException("Cleanup could not delete article " + articleSlug
                        + ": " + deletionResponse.statusLine());
            }
        });
    }

    public void waitForArticleAuthor(Account articleAuthor, String articleSlug) {
        // The upstream API associates the author asynchronously after creating the article.
        await("article " + articleSlug + " is readable and attributed to " + articleAuthor.username())
                .atMost(articleAuthorWait)
                .until(() -> {
                    Response articleResponse = articlesClient.getArticleBySlug(articleSlug);
                    if (articleResponse.statusCode() != HttpStatus.SC_OK) {
                        return false;
                    }
                    Article persistedArticle = readArticleFromResponse(articleResponse);
                    return persistedArticle != null && persistedArticle.getAuthor() != null
                            && articleAuthor.username().equals(persistedArticle.getAuthor().getUsername());
                });
    }

    private static void requireResponseStatus(Response response, int expectedStatus, String operation) {
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(operation + " expected HTTP " + expectedStatus
                    + " but received " + response.statusLine());
        }
    }

    private static Account readFixtureAccount(Response response, Credentials credentials, String operation) {
        User authenticatedUser = readUserFromResponse(response);
        if (authenticatedUser == null || authenticatedUser.getToken() == null
                || authenticatedUser.getToken().isBlank()) {
            throw new IllegalStateException(operation + " returned no access token");
        }
        if (authenticatedUser.getUsername() == null || authenticatedUser.getUsername().isBlank()) {
            throw new IllegalStateException(operation + " returned no username");
        }
        return new Account(authenticatedUser.getUsername(), credentials, authenticatedUser.getToken());
    }

    @Override
    public void close() {
        IllegalStateException cleanupFailures = new IllegalStateException("Conduit fixture cleanup failed");
        while (!pendingCleanupActions.isEmpty()) {
            try {
                pendingCleanupActions.pop().run();
            } catch (RuntimeException cleanupFailure) {
                cleanupFailures.addSuppressed(cleanupFailure);
            }
        }
        if (cleanupFailures.getSuppressed().length > 0) {
            throw cleanupFailures;
        }
    }

    public record Account(String username, Credentials credentials, String accessToken) {
        @Override
        public String toString() {
            return "Account[username=" + username + "]";
        }
    }
}
