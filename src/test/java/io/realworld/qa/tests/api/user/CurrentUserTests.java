package io.realworld.qa.tests.api.user;

import io.realworld.qa.api.model.common.User;
import io.realworld.qa.support.ConduitTest;
import io.realworld.qa.support.Scenario.Account;
import io.realworld.qa.support.TestAccountRole;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.realworld.qa.api.ApiUtils.readCurrentUserFromResponse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("api")
@Tag("regression")
class CurrentUserTests extends ConduitTest {
    @Test
    @Tag("smoke")
    @DisplayName("An authenticated user can retrieve their own profile")
    void getAuthenticatedUserProfile() {
        Account registeredAccount = scenario.registerTestAccount(TestAccountRole.PROFILE_OWNER);
        Account authenticatedAccount = scenario.logInTestAccount(registeredAccount);
        Response currentUserResponse = conduitApi.user().getCurrentUser(authenticatedAccount.accessToken());
        assertEquals(HttpStatus.SC_OK, currentUserResponse.statusCode(),
                "GET /user should accept the token issued at login");

        User currentUser = readCurrentUserFromResponse(currentUserResponse);
        assertNotNull(currentUser, "GET /user should return the authenticated user object");
        assertAll("Authenticated profile",
                () -> assertEquals(authenticatedAccount.username(), currentUser.getUsername(),
                        "The authenticated profile should match the login username"),
                () -> assertEquals(authenticatedAccount.credentials().getEmail(), currentUser.getEmail(),
                        "The authenticated profile should match the login email"));
    }
}
