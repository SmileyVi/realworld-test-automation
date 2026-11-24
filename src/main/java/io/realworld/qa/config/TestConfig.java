package io.realworld.qa.config;

import io.realworld.qa.ui.browser.BrowserType;

import java.net.URI;

import static io.realworld.qa.config.ConfigValues.httpBaseUri;
import static io.realworld.qa.config.ConfigValues.setting;

public record TestConfig(URI webUrl, boolean headless, BrowserType browser) {
    public static TestConfig load() {
        return new TestConfig(
                httpBaseUri("conduit.webUrl", setting("conduit.webUrl", "CONDUIT_WEB_URL", "http://127.0.0.1:8080")),
                Boolean.parseBoolean(setting("conduit.headless", "CONDUIT_HEADLESS", "true")),
                BrowserType.fromConfig(setting("conduit.browser", "CONDUIT_BROWSER", "chrome")));
    }
}
