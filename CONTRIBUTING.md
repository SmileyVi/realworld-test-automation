# Contributing

Start Conduit and run the suite using the [README](README.md). The tests target a pinned [Conduit implementation](https://github.com/TonyMckes/conduit-realworld-example-app/tree/5e127d8569b300e0a21dc2c20ea680da4967b1aa). Use a disposable instance because the suite creates users and articles.

## Project layout

| Location | Contents |
| --- | --- |
| [`src/main/java/io/realworld/qa/api`](src/main/java/io/realworld/qa/api) | REST Assured clients, Jackson models, and response mapping |
| [`src/main/java/io/realworld/qa/config`](src/main/java/io/realworld/qa/config) | Runtime settings and service readiness checks |
| [`src/main/java/io/realworld/qa/ui`](src/main/java/io/realworld/qa/ui) | Selenide page objects and the browser factory |
| [`src/test/java/io/realworld/qa`](src/test/java/io/realworld/qa) | API and UI scenarios, fixtures, assertions, and browser lifecycle |
| [`src/test/resources`](src/test/resources) | JSON test data, JUnit parallel execution, and Allure settings |

## Add or change tests

### API coverage

Match client and test package names to the backend controllers. The [Conduit route registrations](https://github.com/TonyMckes/conduit-realworld-example-app/blob/5e127d8569b300e0a21dc2c20ea680da4967b1aa/backend/index.js) show which controller handles each endpoint.

| Route family | Client | Test package |
| --- | --- | --- |
| `/api/articles` | `ArticlesClient` | `tests.api.articles` |
| `/api/users` and `/api/users/login` | `UsersClient` | `tests.api.users` |
| `/api/user` | `UserClient` | `tests.api.user` |

Access clients through `ConduitApi` and use `ConduitRequests` for shared request configuration. Keep controller-specific request and response models with their API packages. Put shared payloads such as `User` in `model/common`. Keep response mapping in `ApiUtils` and assertions in the tests. Reuse existing fixtures and configuration keys when they cover the operation.

### UI coverage and test data

Use the JUnit `BrowserSession` extension to give each invocation its own browser session. Pass its Selenide driver to page objects. Use the API fixtures for prerequisite data and article cleanup. Each invocation registers its own author, and the cross-user journey registers a separate reader.

Add a named object to the [article journeys dataset](src/test/resources/test-data/articles/journeys.json) to run it through public reading, signed-in reading, and editing. Each case supplies draft and edit inputs and the expected rendered text. The fixture appends a unique title suffix to isolate concurrent invocations. Awaitility waits for Conduit to persist the article's author association. The upstream API has no account deletion endpoint, so account cleanup happens when the disposable database volume is removed.

For validation scenarios, use a separate dataset or a JUnit method source with a name, exact input, and expected outcome per case. For a title-length boundary, submit the exact title directly to preserve its length.

### Tags and parallel execution

Use `api` or `ui` to identify the suite, `regression` for the full integration suite, and `smoke` for the small subset that verifies core flows. Failsafe applies the JUnit tag expression in `test.groups`.

```sh
mvn "-Dtest.groups=api & smoke" verify
mvn "-Dtest.groups=ui & regression & !smoke" verify
mvn "-Dconduit.parallel.workers=4" verify
```

Maven runs two test cases concurrently by default. Use one worker for serial execution. The GitHub Actions variable `CONDUIT_TEST_WORKERS` controls the worker count in each CI job.

## Runtime configuration

| Environment variable | Maven property | Default |
| --- | --- | --- |
| `CONDUIT_API_URL` | `conduit.apiUrl` | `http://127.0.0.1:8080/api` |
| `CONDUIT_WEB_URL` | `conduit.webUrl` | `http://127.0.0.1:8080` |
| `CONDUIT_BROWSER` | `conduit.browser` | `chrome` |
| `CONDUIT_HEADLESS` | `conduit.headless` | `true` |

Maven properties take precedence over environment variables. To use a different Compose host port, set `CONDUIT_PORT` before starting Compose and update both test URLs.

[API properties](src/main/resources/api.properties) define paths relative to the API base URL, authentication headers, content type, timeouts, mapper, and fixture waits. Override a key for a single run with `-D`, for example `mvn -Papi "-Dapi.http.socketTimeout.millis=15000" verify`.

Use `chrome`, `firefox`, or `edge` as the browser value. Selenium Manager resolves the driver when one is not supplied. To use a local driver, set its absolute path with `webdriver.chrome.driver`, `webdriver.gecko.driver`, or `webdriver.edge.driver`. Set `-Dconduit.headless=false` to watch the run. [Selenide properties](src/main/resources/selenide.properties) define the default browser size and element wait.

## CI and test evidence

The [GitHub Actions workflow](.github/workflows/conduit-tests.yml) runs the API suite before the Chrome, Firefox, and Edge matrix. Each job starts its own Compose stack and uploads Failsafe XML and available Allure results. Failed jobs also collect Conduit logs and, for Edge, available WebDriver startup logs.

Run `mvn allure:report` to generate an HTML report under `target/site/allure-maven-plugin/`. Use `mvn allure:serve` to view the results locally.

To investigate a failure, open the test invocation in Allure and select the **Test data** attachment. It lists the dataset or test name, generated account roles, usernames and emails, and any published article slug. The Maven console logs the same identifiers alongside fixture activity. Record diagnostic fields explicitly to keep passwords, tokens, and request or response bodies out of the evidence.

The console shows service readiness, browser selection, and failures that occur before a browser session or HTTP response. For request-level detail during an API run, use `mvn -Papi "-Dorg.slf4j.simpleLogger.log.io.realworld.qa.api.client.ConduitRequests=debug" verify`. The trace shows the HTTP method, route, status, and elapsed time.
