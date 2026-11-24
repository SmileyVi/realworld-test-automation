package io.realworld.qa.tests.ui;

import io.realworld.qa.api.model.articles.request.ArticleDraft;
import io.realworld.qa.api.model.articles.response.Article;
import io.realworld.qa.api.model.users.request.Credentials;
import io.realworld.qa.config.ConduitEnvironment;
import io.realworld.qa.config.TestConfig;
import io.realworld.qa.support.BrowserSession;
import io.realworld.qa.support.ConduitTest;
import io.realworld.qa.support.Scenario;
import io.realworld.qa.support.Scenario.Account;
import io.realworld.qa.support.data.ArticleJourneyCase;
import io.realworld.qa.support.data.JsonTestData;
import io.realworld.qa.ui.ArticlePage;
import io.realworld.qa.ui.LoginPage;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static io.realworld.qa.api.ApiUtils.readArticleFromResponse;
import static io.realworld.qa.support.Scenario.TEST_USER_ALIAS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("ui")
class ArticlesUITests extends ConduitTest {
    private static final TestConfig CONFIG = TestConfig.load();
    private static final String TEST_READER_ALIAS = "testReader";
    private static Account articleAuthor;

    @RegisterExtension
    final BrowserSession browserSession = new BrowserSession(CONFIG);

    @BeforeAll
    static void prepareArticleAuthor() {
        ConduitEnvironment.awaitWeb(CONFIG);
        try (Scenario setup = newScenario(newConduitApi())) {
            articleAuthor = setup.authenticateConfiguredUser(TEST_USER_ALIAS);
        }
    }

    private static Stream<ArticleJourneyCase> articleJourneys() {
        return JsonTestData.readCases("/test-data/articles/journeys.json", ArticleJourneyCase.class).stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("articleJourneys")
    @DisplayName("A visitor can read an article and see its author")
    void readPublicArticle(ArticleJourneyCase articleCase) {
        PublishedArticleFixture article = publishArticle(articleAuthor, articleCase.draft());

        ArticlePage articlePage = openPublishedArticle(article);
        assertArticleBodyAndAuthor(articlePage, articleCase.draft().expectedRenderedText(), articleAuthor.username());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("articleJourneys")
    @DisplayName("A signed-in reader can view an article another user published")
    void signedInReaderCanViewAnotherUsersArticle(ArticleJourneyCase articleCase) {
        Account articleReader = scenario.authenticateConfiguredUser(TEST_READER_ALIAS);
        assertNotEquals(articleAuthor.username(), articleReader.username(),
                "testUser and testReader must refer to different Conduit accounts");
        PublishedArticleFixture article = publishArticle(articleAuthor, articleCase.draft());

        LoginPage loginPage = signInAs(articleReader);
        ArticlePage articlePage = openPublishedArticle(article);

        assertArticleBodyAndAuthor(articlePage, articleCase.draft().expectedRenderedText(), articleAuthor.username());
        loginPage.signedInUsername().shouldHave(exactText(articleReader.username())
                .because("the second user's session should persist while viewing the article"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("articleJourneys")
    @DisplayName("An author can sign in and edit an existing article")
    void editArticleAsAuthor(ArticleJourneyCase articleCase) {
        PublishedArticleFixture article = publishArticle(articleAuthor, articleCase.draft());
        String articleSlug = article.slug();

        signInAs(articleAuthor);
        ArticlePage articlePage = openPublishedArticle(article);
        ArticleJourneyCase.Edit articleEdit = articleCase.edit();
        articlePage.openEditor().submitArticleUpdate(articleEdit.description(), articleEdit.body());

        articlePage.articleBody().shouldHave(text(articleEdit.expectedRenderedText())
                .because("saving the edit should display the new article body"));
        Response persistedArticleResponse = conduitApi.articles().getArticleBySlug(articleSlug);
        assertEquals(HttpStatus.SC_OK, persistedArticleResponse.statusCode(),
                () -> "The API should still return article " + articleSlug + " after the browser edit");
        Article persistedArticle = readArticleFromResponse(persistedArticleResponse);
        assertNotNull(persistedArticle, () -> "The API response after the browser edit should contain " + articleSlug);
        assertAll("Article content persisted after the browser edit",
                () -> assertEquals(articleEdit.description(), persistedArticle.getDescription(),
                        "The description edited in the browser should persist"),
                () -> assertEquals(articleEdit.body(), persistedArticle.getBody(),
                        "The body edited in the browser should persist"));
    }

    private PublishedArticleFixture publishArticle(Account articleAuthor, ArticleJourneyCase.Draft draftData) {
        ArticleDraft articleDraft = scenario.generateUniqueArticleDraft(draftData.toRequest());
        Article publishedArticle = scenario.createArticleFixture(articleAuthor, articleDraft);
        return new PublishedArticleFixture(publishedArticle.getSlug(), articleDraft.getTitle());
    }

    private LoginPage signInAs(Account account) {
        LoginPage loginPage = new LoginPage(browserSession.driver());
        loginPage.openFromHomePage();
        Credentials credentials = account.credentials();
        loginPage.signIn(credentials.getEmail(), credentials.getPassword());
        loginPage.signedInUsername().shouldHave(exactText(account.username())
                .because("the navigation should show the signed-in account before continuing"));
        return loginPage;
    }

    private ArticlePage openPublishedArticle(PublishedArticleFixture article) {
        ArticlePage articlePage = new ArticlePage(browserSession.driver());
        articlePage.openBySlug(article.slug());
        articlePage.articleTitle().shouldHave(exactText(article.expectedTitle())
                .because("the page should show the title used to publish article " + article.slug()));
        return articlePage;
    }

    private static void assertArticleBodyAndAuthor(ArticlePage articlePage, String expectedBody, String expectedAuthor) {
        articlePage.articleBody().shouldHave(text(expectedBody)
                .because("the article should render the published body"));
        articlePage.authorName().shouldHave(exactText(expectedAuthor)
                .because("the article should credit the account that published it"));
    }

    private record PublishedArticleFixture(String slug, String expectedTitle) {}
}
