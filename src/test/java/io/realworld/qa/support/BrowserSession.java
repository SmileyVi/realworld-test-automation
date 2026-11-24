package io.realworld.qa.support;

import com.codeborne.selenide.SelenideDriver;
import io.qameta.allure.Allure;
import io.realworld.qa.config.TestConfig;
import io.realworld.qa.ui.browser.BrowserFactory;
import io.realworld.qa.ui.browser.BrowserType;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;

public final class BrowserSession implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback {
    private final BrowserType browser;
    private final SelenideDriver driver;

    public BrowserSession(TestConfig config) {
        browser = config.browser();
        driver = BrowserFactory.create(config);
    }

    public SelenideDriver driver() {
        return driver;
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Allure.parameter("browser", browser.selenideName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (!driver.hasWebDriverStarted()) {
            return;
        }
        WebDriver webDriver = driver.getWebDriver();
        if (webDriver instanceof HasCapabilities browserDriver) {
            Capabilities capabilities = browserDriver.getCapabilities();
            Allure.addAttachment("Browser", "text/plain",
                    capabilities.getBrowserName() + " " + capabilities.getBrowserVersion());
        }
        context.getExecutionException().ifPresent(failure -> {
            if (webDriver instanceof TakesScreenshot screenshotDriver) {
                try {
                    Allure.addAttachment("Browser screenshot", "image/png",
                            new ByteArrayInputStream(screenshotDriver.getScreenshotAs(OutputType.BYTES)), ".png");
                } catch (RuntimeException screenshotFailure) {
                    failure.addSuppressed(screenshotFailure);
                }
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) {
        driver.close();
    }
}
