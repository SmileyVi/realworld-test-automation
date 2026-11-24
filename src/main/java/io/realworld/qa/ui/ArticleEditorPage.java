package io.realworld.qa.ui;

import com.codeborne.selenide.SelenideDriver;

public record ArticleEditorPage(SelenideDriver browser) {
    public void submitArticleUpdate(String updatedDescription, String updatedBody) {
        browser.$("form input[name='description']").setValue(updatedDescription);
        browser.$("form textarea[name='body']").setValue(updatedBody);
        browser.$("form button[type='submit']").click();
    }
}
