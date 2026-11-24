package io.realworld.qa.ui.browser;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import io.realworld.qa.config.TestConfig;

public final class BrowserFactory {
    private BrowserFactory() {}

    public static SelenideDriver create(TestConfig config) {
        return new SelenideDriver(new SelenideConfig()
                .baseUrl(config.webUrl().toString())
                .browser(config.browser().selenideName())
                .headless(config.headless())
                .screenshots(false)
                .savePageSource(false));
    }
}
