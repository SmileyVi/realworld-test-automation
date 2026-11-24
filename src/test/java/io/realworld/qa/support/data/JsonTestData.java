package io.realworld.qa.support.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Slf4j
public final class JsonTestData {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private JsonTestData() {}

    public static <T> List<T> readCases(String resource, Class<T> caseType) {
        try (InputStream input = JsonTestData.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Test data file is missing from the classpath: " + resource);
            }
            List<T> cases = JSON.readerForListOf(caseType).readValue(input);
            if (cases == null || cases.isEmpty()) {
                throw new IllegalArgumentException("Test data file must contain at least one case: " + resource);
            }
            log.debug("Loaded {} test cases from {}", cases.size(), resource);
            return List.copyOf(cases);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read test data file " + resource, exception);
        }
    }
}
