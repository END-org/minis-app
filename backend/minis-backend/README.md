# MINIS Backend

Spring Boot modular monolith backend for the MINIS mobile-first mini-film platform.

## Current Baseline

The current backend slice verifies Firebase ID tokens, bootstraps a local MINIS user, exposes `/api/v1/auth/bootstrap` and `/api/v1/me`, returns the shared JSON API error contract, propagates `X-Correlation-ID`, denies local auth-lab/dev routes outside the `local` profile, and creates the identity schema through Flyway.

Product features such as feed, content, media upload, comments, social interactions, notifications, reports, analytics, chat, admin APIs, and React are not implemented in this baseline.

## Prerequisites

- JDK 21 target. The local build can run on newer JDKs when Maven compiles with the Java 21 release target.
- Maven Wrapper 3.3.4, downloading Apache Maven 3.9.16 when needed.
- Docker and Docker Compose.
- Firebase development project.
- Firebase Authentication Email/Password provider enabled.
- Firebase Authentication Google provider enabled.
- Firebase Web App registered.
- Firebase Admin service-account JSON generated and kept outside Git.

## Local Environment

Copy the root environment template:

```bash
cp ../../.env.example ../../.env
```

Spring Boot reads normal environment variables. Docker Compose reads the root `.env` file.

The default local database name is `minis_event_local`, which is intentionally separate from older pre-Flyway development databases.

Private Admin credential:

```text
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/outside/repo/minis-dev-firebase-admin.json
```

Public Firebase Web SDK values:

```text
FIREBASE_PROJECT_ID
FIREBASE_WEB_API_KEY
FIREBASE_WEB_AUTH_DOMAIN
FIREBASE_WEB_APP_ID
FIREBASE_WEB_STORAGE_BUCKET
```

The public web values are safe to send to the browser from the local-only endpoint. The Admin JSON is private and must never be committed, copied into YAML, copied into a Docker image, or served from static resources.

## Commands

From the repository root:

```bash
docker compose -f compose.local.yml up -d
docker compose -f compose.local.yml down
docker compose -f compose.local.yml down -v
docker compose -f compose.demo.yml up -d --build
docker compose -f compose.demo.yml down
```

From `backend/minis-backend`:

```bash
./mvnw spotless:apply
./mvnw --batch-mode --no-transfer-progress verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
./mvnw --batch-mode --no-transfer-progress package
docker build -t minis-backend:local .
```

Windows PowerShell equivalents:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd --batch-mode --no-transfer-progress verify
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Run the local application only after PostgreSQL is available and `GOOGLE_APPLICATION_CREDENTIALS` plus Firebase Web SDK environment variables are set in the shell or IDE run configuration.

The demo compose topology also requires explicit `DEMO_POSTGRES_PASSWORD`, `DEMO_MINIO_ROOT_USER`, `DEMO_MINIO_ROOT_PASSWORD`, `FIREBASE_PROJECT_ID`, and `GOOGLE_APPLICATION_CREDENTIALS` values.

## Local URLs

- API base: `http://localhost:8080`
- Auth test page: `http://localhost:8080/auth-test/index.html`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator readiness: `http://localhost:8080/actuator/health/readiness`
- MinIO console: `http://localhost:9001`
- Demo reverse-proxy health: `http://localhost:8088/healthz`

## Manual Firebase Browser Test

1. Open `http://localhost:8080/auth-test/index.html`.
2. Register with email/password.
3. Send the verification email.
4. Verify the email in Firebase.
5. Reload the Firebase user on the test page.
6. Request a fresh ID token.
7. Call `POST /api/v1/auth/bootstrap`.
8. Call `GET /api/v1/me`.
9. Test Google popup sign-in and repeat bootstrap plus `/me`.

## Common Local Errors

- Invalid `GOOGLE_APPLICATION_CREDENTIALS`: set an absolute path to a real development Firebase Admin JSON outside the repository.
- Wrong Firebase project: make sure `FIREBASE_PROJECT_ID` matches the Admin credential and Web App.
- Email/Password provider disabled: enable it in Firebase Authentication.
- Google provider disabled: enable it and set authorized domains for localhost.
- Email not verified: send the verification email, verify, reload the Firebase user, then request a fresh token.
- PostgreSQL unavailable: run `docker compose -f compose.local.yml up -d` from the repository root.
- Environment variables not loaded: export them in the shell or configure them in the IDE run configuration.

## Quality Gates

`./mvnw verify` runs compilation, unit tests, security tests, PostgreSQL/Testcontainers integration tests, ArchUnit rules, Spotless check, Checkstyle, PMD, SpotBugs, Maven Enforcer, and Maven dependency analysis.

Error Prone is not configured in this first slice because the local build is running on JDK 23 while the project targets Java 21, and the Maven compiler/Error Prone module-export setup would make the build less stable than the rest of the gates. SpotBugs, PMD, Checkstyle, dependency convergence, dependency analysis, and ArchUnit remain active in `verify`.

## Database Initialization

Flyway is the source of truth for the physical schema. The initial identity migration is `src/main/resources/db/migration/V1__create_identity_user_accounts.sql`.

- `local`: Flyway migrates the local PostgreSQL database and Hibernate validates it.
- `test`: Flyway migrates PostgreSQL Testcontainers databases and Hibernate validates them.
- `demo`: Flyway migrates the demo PostgreSQL service and Hibernate validates it.
- `prod`: Flyway migrates the configured PostgreSQL database and Hibernate validates it.

Hibernate schema generation modes `update`, `create`, and `create-drop` are not used by application profiles. Flyway `baselineOnMigrate` is not enabled globally, and Flyway clean is disabled during application startup.

### Old-Volume Policy

Phase 1 expects a dedicated clean event database. Unknown Hibernate-created schemas are not adopted silently. Recreate disposable local PostgreSQL and MinIO volumes with:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

Recreate disposable demo volumes with:

```bash
docker compose -f compose.demo.yml down -v
docker compose -f compose.demo.yml up -d --build
```

Preserving an old database requires a separate backup, migration rehearsal, and Flyway baseline decision before pointing the application at it.

## Environment Assumptions

- Docker Desktop is available for Testcontainers and local PostgreSQL/MinIO.
- Firebase Admin credentials are stored outside Git and referenced with `GOOGLE_APPLICATION_CREDENTIALS`.
- Public Firebase Web SDK values are supplied through environment variables for the local auth test page.
- No production secrets are stored in `.env.example`, YAML files, tests, static resources, or Docker images.

## Local Pre-Push Hook

Enable:

```bash
git config core.hooksPath .githooks
```

Disable:

```bash
git config --unset core.hooksPath
```

Git hooks are local and are not enabled automatically after cloning. A developer can bypass them with `--no-verify`. GitHub Actions is therefore the authoritative shared quality gate. CI runs after code is pushed; it does not erase or prevent the remote branch push.

## GitHub Branch Protection

Repository maintainers still need to configure this manually in GitHub:

- Require pull requests before merging into protected production branches.
- Disable direct pushes to the protected production branch where appropriate.
- Require the `Backend quality gate` status check.
- Require branches to be up to date before merge if that fits the team workflow.
- Do not allow bypassing required status checks except for explicitly trusted administrators.
