package io.realworld.qa.support;

import io.realworld.qa.api.client.ConduitApi;
import io.realworld.qa.config.ApiConfig;
import io.realworld.qa.config.ConduitEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public abstract class ConduitTest {
    private static final ApiConfig API_CONFIG = ApiConfig.load();
    private static final TestUsers TEST_USERS = TestUsers.load();
    protected final ConduitApi conduitApi = newConduitApi();
    protected Scenario scenario;

    @BeforeAll
    static void awaitApi() {
        ConduitEnvironment.awaitApi(API_CONFIG);
    }

    @BeforeEach
    void openScenario() {
        scenario = newScenario(conduitApi);
    }

    protected static ConduitApi newConduitApi() {
        return new ConduitApi(API_CONFIG);
    }

    protected static Scenario newScenario(ConduitApi api) {
        return new Scenario(api.users(), api.articles(), API_CONFIG.articleAuthorWait(), TEST_USERS);
    }

    @AfterEach
    void cleanFixtures() {
        if (scenario != null) {
            scenario.close();
        }
    }
}
