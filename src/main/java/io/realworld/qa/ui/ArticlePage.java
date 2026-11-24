package io.realworld.qa.ui;

import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideElement;

public record ArticlePage(SelenideDriver browser) {
    public void openBySlug(String articleSlug) {
        browser.open("/#/article/" + articleSlug);
    }

    public SelenideElement articleTitle() {
        return browser.$(".article-page h1");
    }

    public SelenideElement articleBody() {
        return browser.$(".article-page .article-content");
    }

    public SelenideElement authorName() {
        return browser.$(".article-page .banner .article-meta a.author");
    }

    public ArticleEditorPage openEditor() {
        browser.$(".article-page .banner a[href^='#/editor/']").click();
        return new ArticleEditorPage(browser);
    }
}
