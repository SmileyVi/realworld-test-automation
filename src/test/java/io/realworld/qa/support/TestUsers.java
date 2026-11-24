package io.realworld.qa.support;

import io.realworld.qa.api.model.users.request.Credentials;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

final class TestUsers {
    private static final String RESOURCE = "/test-users.properties";

    private final Properties accountMappings;
    private final Function<String, String> environment;

    TestUsers(Properties accountMappings, Function<String, String> environment) {
        this.accountMappings = accountMappings;
        this.environment = environment;
    }

    static TestUsers load() {
        try (InputStream configStream = TestUsers.class.getResourceAsStream(RESOURCE)) {
            if (configStream == null) {
                throw new IllegalStateException("Missing test user configuration " + RESOURCE);
            }
            Properties accountMappings = new Properties();
            accountMappings.load(configStream);
            return new TestUsers(accountMappings, System::getenv);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read test user configuration " + RESOURCE, exception);
        }
    }

    Optional<Credentials> findCredentials(String alias) {
        String emailVariable = environmentVariable(alias + ".email.env");
        String passwordVariable = environmentVariable(alias + ".password.env");
        String email = environment.apply(emailVariable);
        String password = environment.apply(passwordVariable);
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPassword = password != null && !password.isEmpty();
        if (hasEmail != hasPassword) {
            throw new IllegalStateException("Set both " + emailVariable + " and " + passwordVariable
                    + " for test user '" + alias + "'");
        }
        return hasEmail ? Optional.of(new Credentials(email.strip(), password)) : Optional.empty();
    }

    Credentials requireCredentials(String alias) {
        return findCredentials(alias).orElseThrow(() -> new IllegalStateException(
                "Test user '" + alias + "' needs credentials in the environment variables listed in " + RESOURCE));
    }

    private String environmentVariable(String property) {
        String name = accountMappings.getProperty(property, "").strip();
        if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(RESOURCE + " must map " + property + " to an environment variable name");
        }
        return name;
    }
}
