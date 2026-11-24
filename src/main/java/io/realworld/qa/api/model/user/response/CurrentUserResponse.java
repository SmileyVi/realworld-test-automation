package io.realworld.qa.api.model.user.response;

import io.realworld.qa.api.model.common.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class CurrentUserResponse {
    private User user;
}
