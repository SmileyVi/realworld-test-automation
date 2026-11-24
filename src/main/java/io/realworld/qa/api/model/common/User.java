package io.realworld.qa.api.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class User {
    private String bio;
    private String email;
    private String image;
    private String token;
    private String username;
}
