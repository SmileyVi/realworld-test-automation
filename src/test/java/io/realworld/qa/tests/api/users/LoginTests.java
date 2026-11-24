package io.realworld.qa.tests.api.users;

import io.realworld.qa.api.client.UsersClient;
import io.realworld.qa.api.model.common.User;
import io.realworld.qa.api.model.users.request.Credentials;
import io.realworld.qa.support.ConduitTest;
import io.realworld.qa.support.Scenario.Account;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.realworld.qa.api.ApiUtils.readErrorsFromResponse;
import static io.realworld.qa.api.ApiUtils.readUserFromResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("api")
class LoginTests extends ConduitTest {
    private final UsersClient usersClient = conduitApi.users();

    @Test
    @DisplayName("A registered user can sign in")
    void logInWithValidCredentials() {
        Account registeredAccount = scenario.registerTestAccount();
        Response loginResponse = usersClient.logInUser(registeredAccount.credentials());
        assertEquals(HttpStatus.SC_OK, loginResponse.statusCode(),
                "POST /users/login should accept the registered credentials");
        User signedInUser = readUserFromResponse(loginResponse);
        assertNotNull(signedInUser, "Successful login response should contain a user object");
        assertThat(signedInUser.getToken()).as("Successful login should issue an access token").isNotBlank();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLoginPasswords")
    @DisplayName("Invalid passwords are rejected")
    void rejectWrongPassword(String caseName, String invalidPassword) {
        Account registeredAccount = scenario.registerTestAccount();
        Credentials invalidCredentials = new Credentials(registeredAccount.credentials().getEmail(), invalidPassword);
        Response rejectedLoginResponse = usersClient.logInUser(invalidCredentials);
        assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, rejectedLoginResponse.statusCode(),
                () -> "POST /users/login should reject " + caseName);
        assertThat(readErrorsFromResponse(rejectedLoginResponse))
                .as("The rejected login should explain the validation error for %s", caseName)
                .hasEntrySatisfying("body", errorMessages -> assertThat(errorMessages)
                        .as("The response should identify the invalid credential combination for %s", caseName)
                        .contains("Wrong email/password combination"));
    }

    private static Stream<Arguments> invalidLoginPasswords() {
        return Stream.of(
                Arguments.of("an incorrect password", "wrong-password"),
                Arguments.of("an empty password", ""),
                Arguments.of("a whitespace-only password", "   ")
        );
    }
}
