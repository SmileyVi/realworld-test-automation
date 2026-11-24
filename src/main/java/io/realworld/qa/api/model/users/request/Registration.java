package io.realworld.qa.api.model.users.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class Registration {
    private final String username;
    private final String email;
    private final String password;
}
