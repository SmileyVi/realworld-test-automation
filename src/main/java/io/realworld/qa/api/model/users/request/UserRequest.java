package io.realworld.qa.api.model.users.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class UserRequest<T> {
    private final T user;
}
