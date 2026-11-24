package io.realworld.qa.support;

import io.realworld.qa.api.client.ConduitApi;
import io.realworld.qa.config.ApiConfig;
import io.realworld.qa.config.ConduitEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class ConduitTest {
    private static final ApiConfig API_CONFIG = ApiConfig.load();
    protected final ConduitApi conduitApi = new ConduitApi(API_CONFIG);
    protected Scenario scenario;

    @BeforeAll
    static void awaitApi() {
        ConduitEnvironment.awaitApi(API_CONFIG);
    }

    @BeforeEach
    void openScenario(TestInfo testInfo) {
        String testName = getClass().getSimpleName() + "." + testInfo.getTestMethod().orElseThrow().getName()
                + " / " + testInfo.getDisplayName();
        scenario = new Scenario(conduitApi.users(), conduitApi.articles(), API_CONFIG.articleAuthorWait(), testName);
    }

    @AfterEach
    void cleanFixtures() {
        if (scenario != null) {
            try {
                scenario.close();
            } finally {
                scenario.attachTestData();
            }
        }
    }
}
