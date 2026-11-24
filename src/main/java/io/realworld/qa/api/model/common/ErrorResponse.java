package io.realworld.qa.api.model.common;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public final class ErrorResponse {
    private Map<String, List<String>> errors;
}
