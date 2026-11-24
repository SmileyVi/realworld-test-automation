package io.realworld.qa.support;

import io.realworld.qa.api.model.users.request.Credentials;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static io.realworld.qa.support.Scenario.TEST_USER_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestUsersTest {
    @Test
    void resolvesCredentialsForTheRequestedAlias() {
        TestUsers testUsers = testUsersWithEnvironment(Map.of(
                "TEST_USER_EMAIL", "user@example.test",
                "TEST_USER_PASSWORD", "user-test-value",
                "EDITOR_EMAIL", " editor@example.test ",
                "EDITOR_PASSWORD", " editor-test-value "));

        Credentials editorCredentials = testUsers.requireCredentials("editor");

        assertEquals("editor@example.test", editorCredentials.getEmail(),
                "The editor alias should resolve its own email");
        assertEquals(" editor-test-value ", editorCredentials.getPassword(),
                "The password must retain its leading and trailing whitespace");
        assertEquals("user@example.test", testUsers.requireCredentials(TEST_USER_ALIAS).getEmail(),
                "Selecting the editor should leave the testUser account available");
    }

    @Test
    void missingCredentialsAreOptionalOnlyWhenTheCallerAllowsIt() {
        TestUsers testUsers = testUsersWithEnvironment(Map.of());

        assertTrue(testUsers.findCredentials(TEST_USER_ALIAS).isEmpty(),
                "The optional lookup should allow testUser credentials to be absent");
        IllegalStateException missingCredentials = assertThrows(IllegalStateException.class,
                () -> testUsers.requireCredentials("editor"),
                "An explicitly selected account should require credentials");
        assertThat(missingCredentials).hasMessageContaining("'editor'");
    }

    @Test
    void partialCredentialsReportVariableNamesWithoutTheirValues() {
        TestUsers testUsers = testUsersWithEnvironment(Map.of("EDITOR_PASSWORD", "unpaired-test-value"));

        IllegalStateException partialCredentials = assertThrows(IllegalStateException.class,
                () -> testUsers.findCredentials("editor"),
                "A partially configured account should fail before login");
        assertThat(partialCredentials.getMessage())
                .contains("EDITOR_EMAIL", "EDITOR_PASSWORD")
                .doesNotContain("unpaired-test-value");
    }

    private static TestUsers testUsersWithEnvironment(Map<String, String> environment) {
        Properties mappings = new Properties();
        mappings.setProperty(TEST_USER_ALIAS + ".email.env", "TEST_USER_EMAIL");
        mappings.setProperty(TEST_USER_ALIAS + ".password.env", "TEST_USER_PASSWORD");
        mappings.setProperty("editor.email.env", "EDITOR_EMAIL");
        mappings.setProperty("editor.password.env", "EDITOR_PASSWORD");
        return new TestUsers(mappings, environment::get);
    }
}
