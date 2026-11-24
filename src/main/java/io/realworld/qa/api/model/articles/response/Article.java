package io.realworld.qa.api.model.articles.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Article {
    private String slug;
    private String title;
    private String description;
    private String body;
    private List<String> tagList;
    private Author author;
}
