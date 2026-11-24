# RealWorld Conduit test automation

A Java 27 and JUnit 5 portfolio project for testing the RealWorld Conduit application. The API suite uses REST Assured, Jackson, and AssertJ. The browser suite uses Selenium 4 through Selenide, with API fixtures for setup and cleanup. Docker Compose runs a pinned [Conduit implementation](https://github.com/TonyMckes/conduit-realworld-example-app/tree/5e127d8569b300e0a21dc2c20ea680da4967b1aa) and PostgreSQL locally and in GitHub Actions.

## Run locally

Use JDK 27, Maven 3.8.6 or newer, and Docker with Compose v2. The browser suite supports Chrome, Mozilla Firefox, and Microsoft Edge. Install the browser you want to run. Selenium Manager resolves its WebDriver when no driver is supplied.

```powershell
docker compose up --build --wait --wait-timeout 120
mvn -Papi verify
& ./scripts/Provision-UiTestUsers.ps1
mvn -Pui verify
mvn allure:report
```

With the UI account environment variables set, run `mvn clean verify` to start with fresh results and execute both suites in one Maven invocation. The API and UI suites run during Maven's `verify` lifecycle via Failsafe. Run `mvn allure:serve` to open the report locally, or serve the generated HTML directory from `target/site/allure-maven-plugin/`.

Run `docker compose down -v` when finished to remove the disposable test database and its generated accounts. Use `docker compose down` to retain local data between sessions.

## Configuration

| Environment variable | Maven property | Default |
| --- | --- | --- |
| `CONDUIT_API_URL` | `conduit.apiUrl` | `http://127.0.0.1:8080/api` |
| `CONDUIT_WEB_URL` | `conduit.webUrl` | `http://127.0.0.1:8080` |
| `CONDUIT_BROWSER` | `conduit.browser` | `chrome` |
| `CONDUIT_HEADLESS` | `conduit.headless` | `true` |

Maven properties take precedence over environment variables. To test a separately hosted Conduit instance, set both URLs. The UI selectors and API status expectations are based on the pinned implementation. Use a disposable environment because the suites create users and articles.

API defaults live in [`src/main/resources/api.properties`](src/main/resources/api.properties). Route paths are relative to the API base URL. The file also defines the auth header and scheme, JSON media type and mapper, Apache HTTP timeout parameter names and values, and readiness and fixture waits. Any key in the file can be overridden with a Maven `-D` property. For example, `mvn -Papi -Dapi.http.socketTimeout.millis=15000 verify` changes the socket timeout for that run. The API base URL resolves in this order: `-Dconduit.apiUrl`, `CONDUIT_API_URL`, then the file default.

To change the Compose host port, set `CONDUIT_PORT` before starting Compose and update both test URLs.

### Browser selection

Run the same UI journeys on one browser per Maven invocation. These PowerShell commands select each supported browser:

```powershell
mvn -Pui '-Dconduit.browser=chrome' verify
mvn -Pui '-Dconduit.browser=firefox' verify
mvn -Pui '-Dconduit.browser=edge' verify
```

Add `'-Dconduit.headless=false'` to see the browser. To use a provisioned WebDriver, pass its absolute path with `webdriver.chrome.driver`, `webdriver.gecko.driver`, or `webdriver.edge.driver`, matching the selected browser.

Selenide reads the browser window size and default element wait from [`selenide.properties`](src/main/resources/selenide.properties). Override either setting for a single run, for example `mvn -Pui '-Dselenide.browserSize=1280x800' '-Dselenide.timeout=15000' verify`.

### Test account and parallel execution

[Test user configuration](src/test/resources/test-users.properties) maps named accounts to environment variables. The browser suite authenticates `testUser` once before the journeys. The cross-user journey also requires a distinct `testReader` account.

| Alias | Environment variables | Use |
| --- | --- | --- |
| `testUser` | `CONDUIT_TEST_USER_EMAIL`, `CONDUIT_TEST_USER_PASSWORD` | Publishes articles and signs in to edit them |
| `testReader` | `CONDUIT_TEST_READER_EMAIL`, `CONDUIT_TEST_READER_PASSWORD` | Signs in to read another user's article |

For a disposable local database, run `& ./scripts/Provision-UiTestUsers.ps1` in PowerShell 7. The script creates each missing account once and sets its credentials for that shell. To reuse your own existing accounts, load the four variables from your password manager or enter them interactively in PowerShell 7:

```powershell
$env:CONDUIT_TEST_USER_EMAIL = Read-Host 'Conduit test account email'
$env:CONDUIT_TEST_USER_PASSWORD = Read-Host 'Conduit test account password' -MaskInput
$env:CONDUIT_TEST_READER_EMAIL = Read-Host 'Conduit reader account email'
$env:CONDUIT_TEST_READER_PASSWORD = Read-Host 'Conduit reader account password' -MaskInput
try {
    mvn -Pui verify
} finally {
    Remove-Item Env:CONDUIT_TEST_USER_EMAIL, Env:CONDUIT_TEST_USER_PASSWORD
    Remove-Item Env:CONDUIT_TEST_READER_EMAIL, Env:CONDUIT_TEST_READER_PASSWORD
}
```

Each GitHub Actions browser job provisions its two users once in its fresh Compose database. For CI runs against a pre-provisioned Conduit environment, map CI secrets to the variables in the table. To add another account, define its alias and secret references:

```properties
editor.email.env=CONDUIT_EDITOR_EMAIL
editor.password.env=CONDUIT_EDITOR_PASSWORD
```

A test can call `scenario.authenticateConfiguredUser("editor")` to sign in as that exact account. Missing credentials fail that test with the alias in the error. The alias identifies an account provisioned with the permissions the test requires. Each test owns its article data and cleanup, and each browser test owns its session.

Maven allows two concurrent test cases by default. Set a positive worker count with `mvn '-Dconduit.parallel.workers=4' verify` in PowerShell, or use `1` for serial execution. The GitHub Actions repository variable `CONDUIT_TEST_WORKERS` controls the worker count within the API job and each browser job. API tests can still generate accounts when needed, including for registration, login, and ownership checks.

## Coverage

| Suite | Scenarios |
| --- | --- |
| API | Registration, sign-in and current profile, rejected incorrect, empty, and whitespace-only passwords, article CRUD, and rejection of deletion by a second user |
| UI | Public article and author display, signed-in reading of another user's article, and owner editing with API verification. Each journey runs with plain text and multiline Markdown data. |

### Article test data

[Article journeys JSON](src/test/resources/test-data/articles/journeys.json) stores named cases, their draft and edit inputs, and the text the browser should render. JSON fits these cases because it can represent multiline strings, arrays, and structured expectations in the same file. CSV is useful for flat input tables, and plain text is useful for standalone article bodies.

Add an object with a unique case name to run another dataset through all three article journeys. JUnit creates a separate invocation per case, and the fixture appends a unique suffix to the title before creating the article so parallel runs can share a database. The browser checks the expected rendered text, while the edit journey checks the original Markdown through the API after saving.

Use a separate dataset for validation scenarios. Each case can hold the exact submitted input and its expected outcome, such as an empty title and its validation message. For title-length boundaries, submit the specified characters directly so a fixture suffix cannot change the length. Build any required valid setup through the existing fixtures.

## Project structure

| Location | Contents |
| --- | --- |
| `src/main/java/io/realworld/qa/api` | Clients grouped by backend controller in `client`, response mapping, and Jackson POJOs in `model/articles`, `model/users`, and `model/user` |
| `src/main/java/io/realworld/qa/config` | Runtime settings and service readiness checks |
| `src/main/java/io/realworld/qa/ui` | Selenide page objects and the browser factory |
| `src/main/resources/api.properties` | API routes, transport defaults, and wait timings |
| `src/test/java/io/realworld/qa` | JUnit scenarios with their assertions, fixtures, and browser lifecycle |
| `src/test/resources` | Test user secret references, JUnit parallel execution, and Allure settings |

### Backend routes and test clients

The backend mounts its routes in [`backend/index.js`](https://github.com/TonyMckes/conduit-realworld-example-app/blob/5e127d8569b300e0a21dc2c20ea680da4967b1aa/backend/index.js). Client names follow the controller names, and each API test package follows the controller it verifies.

| Backend controller or route | Route family | Framework mapping |
| --- | --- | --- |
| `controllers/articles.js` | `/api/articles`, including `/:slug` and `/feed` | `ArticlesClient` and `tests.api.articles` cover article CRUD |
| `controllers/users.js` | `/api/users` and `/api/users/login` | `UsersClient` and `tests.api.users` cover registration and login |
| `controllers/user.js` | `/api/user` | `UserClient` and `tests.api.user` cover reading the current user |
| `controllers/comments.js` | `/api/articles/:slug/comments` | Add a separate comments client and test package when covering comments |
| `controllers/favorites.js` | `/api/articles/:slug/favorite` | Add a separate favorites client and test package when covering favorites |
| `controllers/profiles.js` | `/api/profiles/:username` and `/follow` | Add a profiles client and test package when covering profiles |
| `routes/tags.js` | `/api/tags` | Used by the API readiness check |

`ConduitApi` gives tests and fixtures access to the controller clients. Each client uses `ConduitRequests` to create a fresh request with the shared base URL, Jackson configuration, timeouts, token header, and Allure HTTP metadata. Response mapping stays in `ApiUtils`, and assertions stay in the tests.

Each controller client owns its request models and response envelopes. Registration, login, and current-profile responses use the same `User` payload from `model/common`, which also holds the backend's common error envelope.

Lombok generates request constructors and getters, and response getters and setters. Maven configures Lombok annotation processing for Java 27.

The browser factory creates a fresh Selenide driver configuration for each test. Selenide selects the browser-specific WebDriver, and the JUnit browser extension owns the session and closes it after the test. Page objects receive that session as a dependency.

For new framework changes, reuse existing configuration keys, classes, methods, and variables wherever they fit. Use a library's built-in configuration when it already covers the setting.

Fixture accounts and article titles use unique generated identifiers. Awaitility handles service readiness and the upstream article-to-author association that is written asynchronously after article creation. Articles created as fixtures are deleted through the API. The upstream API has no account deletion endpoint, so stopping Compose with `-v` removes test accounts.

For input validation, use a JUnit parameterized test with named cases. In `LoginTests`, each row in `invalidLoginPasswords` runs separately, and the test generates a fresh account for that row. Add a row to the method source to cover another password. When cases have different outcomes, include the expected HTTP status and error message in each row.

## GitHub Actions and reports

The API job gates a parallel Chrome, Firefox, and Edge matrix. Each job starts a fresh Compose stack and uploads its own test evidence, including Failsafe XML and, when results exist, the Allure HTML report and raw results. An Allure API attachment records the operation, HTTP status, and duration. UI results include the browser name and version, with a screenshot on failure. Failed jobs also collect Conduit logs.
