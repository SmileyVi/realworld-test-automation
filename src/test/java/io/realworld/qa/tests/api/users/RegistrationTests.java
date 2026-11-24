package io.realworld.qa.tests.api.users;

import io.realworld.qa.api.model.common.User;
import io.realworld.qa.api.model.users.request.Registration;
import io.realworld.qa.support.ConduitTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.realworld.qa.api.ApiUtils.readUserFromResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("api")
class RegistrationTests extends ConduitTest {
    @Test
    @DisplayName("A new user can register an account")
    void registerNewAccount() {
        Registration registration = scenario.generateUniqueRegistration();
        Response registrationResponse = conduitApi.users().registerUser(registration);
        assertEquals(HttpStatus.SC_CREATED, registrationResponse.statusCode(),
                "POST /users should register a new account");

        User registeredUser = readUserFromResponse(registrationResponse);
        assertNotNull(registeredUser, "Registration response should contain a user object");
        assertAll("Registered user",
                () -> assertEquals(registration.getUsername(), registeredUser.getUsername(),
                        "Registration should return the submitted username"),
                () -> assertEquals(registration.getEmail(), registeredUser.getEmail(),
                        "Registration should return the submitted email"),
                () -> assertThat(registeredUser.getToken())
                        .as("Registration should issue an access token").isNotBlank());
    }
}
