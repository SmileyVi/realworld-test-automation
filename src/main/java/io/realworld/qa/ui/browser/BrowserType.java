package io.realworld.qa.ui.browser;

import com.codeborne.selenide.Browsers;

import java.util.Locale;

public enum BrowserType {
    CHROME(Browsers.CHROME),
    FIREFOX(Browsers.FIREFOX),
    EDGE(Browsers.EDGE);

    private final String selenideName;

    BrowserType(String selenideName) {
        this.selenideName = selenideName;
    }

    public static BrowserType fromConfig(String name) {
        try {
            return valueOf(name.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "conduit.browser (CONDUIT_BROWSER) must be chrome, firefox, or edge. Received: " + name,
                    exception);
        }
    }

    public String selenideName() {
        return selenideName;
    }
}
