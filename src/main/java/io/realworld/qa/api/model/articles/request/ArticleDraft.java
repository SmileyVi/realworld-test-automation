package io.realworld.qa.api.model.articles.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public final class ArticleDraft {
    private final String title;
    private final String description;
    private final String body;
    private final List<String> tagList;
}
