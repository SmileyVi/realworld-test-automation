package io.realworld.qa.api.model.users.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class Credentials {
    private final String email;
    private final String password;
}
