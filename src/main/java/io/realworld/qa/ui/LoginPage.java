package io.realworld.qa.ui;

import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideElement;

public record LoginPage(SelenideDriver browser) {
    public void openFromHomePage() {
        browser.open("/");
        browser.$("nav a[href='#/login']").click();
    }

    public void signIn(String email, String password) {
        browser.$("form input[name='email']").setValue(email);
        browser.$("form input[name='password']").setValue(password);
        browser.$("form button").click();
    }

    public SelenideElement signedInUsername() {
        return browser.$("nav .dropdown-toggle");
    }
}
