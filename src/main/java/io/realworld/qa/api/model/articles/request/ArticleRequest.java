package io.realworld.qa.api.model.articles.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ArticleRequest<T> {
    private final T article;
}
