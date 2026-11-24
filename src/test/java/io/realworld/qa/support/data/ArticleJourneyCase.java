package io.realworld.qa.support.data;

import io.realworld.qa.api.model.articles.request.ArticleDraft;

import java.util.List;

public record ArticleJourneyCase(String name, Draft draft, Edit edit) {
    public record Draft(String title, String description, String body, List<String> tagList,
                        String expectedRenderedText) {
        public ArticleDraft toRequest() {
            return new ArticleDraft(title, description, body, tagList);
        }
    }

    public record Edit(String description, String body, String expectedRenderedText) {}

    @Override
    public String toString() {
        return name;
    }
}
