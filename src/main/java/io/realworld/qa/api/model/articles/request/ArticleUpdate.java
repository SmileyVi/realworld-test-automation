package io.realworld.qa.api.model.articles.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ArticleUpdate {
    private final String description;
    private final String body;
}
