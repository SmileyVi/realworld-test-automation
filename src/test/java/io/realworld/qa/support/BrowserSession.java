package io.realworld.qa.support;

import com.codeborne.selenide.SelenideDriver;
import io.qameta.allure.Allure;
import io.realworld.qa.config.TestConfig;
import io.realworld.qa.ui.browser.BrowserFactory;
import io.realworld.qa.ui.browser.BrowserType;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public final class BrowserSession implements BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback {
    private final BrowserType browser;
    private final boolean headless;
    private final SelenideDriver driver;

    public BrowserSession(TestConfig config) {
        browser = config.browser();
        headless = config.headless();
        driver = BrowserFactory.create(config);
    }

    public SelenideDriver driver() {
        return driver;
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        log.info("[{}] Browser configured: {}, headless={}", testName(context), browser.selenideName(), headless);
        Allure.parameter("browser", browser.selenideName());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (!driver.hasWebDriverStarted()) {
            context.getExecutionException().ifPresent(failure ->
                    log.warn("[{}] Test failed without a {} session ({})",
                            testName(context), browser.selenideName(), failure.getClass().getSimpleName()));
            return;
        }
        WebDriver webDriver = driver.getWebDriver();
        if (webDriver instanceof HasCapabilities browserDriver) {
            Capabilities capabilities = browserDriver.getCapabilities();
            log.info("[{}] Browser session used {} {}", testName(context),
                    capabilities.getBrowserName(), capabilities.getBrowserVersion());
            Allure.addAttachment("Browser", "text/plain",
                    capabilities.getBrowserName() + " " + capabilities.getBrowserVersion());
        }
        context.getExecutionException().ifPresent(failure -> {
            if (webDriver instanceof TakesScreenshot screenshotDriver) {
                try {
                    Allure.addAttachment("Browser screenshot", "image/png",
                            new ByteArrayInputStream(screenshotDriver.getScreenshotAs(OutputType.BYTES)), ".png");
                } catch (RuntimeException screenshotFailure) {
                    log.warn("[{}] Could not capture a browser screenshot ({})",
                            testName(context), screenshotFailure.getClass().getSimpleName());
                    failure.addSuppressed(screenshotFailure);
                }
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) {
        driver.close();
    }

    private static String testName(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName()
                + " / " + context.getDisplayName();
    }
}
