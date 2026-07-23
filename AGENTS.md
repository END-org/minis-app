# MINIS Repository Engineering Rules

## Stack

- Backend: Java 21 target, Spring Boot, PostgreSQL, Firebase Authentication, OpenAPI, ArchUnit, Testcontainers, Spotless, Checkstyle, PMD, SpotBugs, Maven Enforcer, and Maven dependency analysis.
- Current Java package root: `com.end.minisbackend`.
- Current clients: placeholders under `mobile/flutter` and `web/admin`; the React event client is not present yet.
- Local infrastructure: PostgreSQL and MinIO are defined in `compose.local.yml`.

## Architecture

- Keep the backend a modular monolith. Do not introduce microservices, Kafka, RabbitMQ, Redis, Elasticsearch, GraphQL, event sourcing, CQRS infrastructure, or plugin frameworks.
- Use package-by-feature: `api`, `application`, `domain`, and `infrastructure` packages only where real code belongs.
- Do not create global controller, service, repository, entity, DTO, mapper, helper, or utility packages for unrelated features.
- Cross-feature code must use narrow application interfaces, explicit queries, or explicit ports. Do not inject another feature's internal repository.
- `shared` is for small cross-cutting primitives only: error contracts, correlation IDs, security plumbing, configuration, and similar infrastructure.

## Dependency Direction

- `api` owns controllers and HTTP request/response contracts.
- `application` owns use-case orchestration, authorization coordination, and transaction boundaries.
- `domain` owns meaningful state, invariants, and transitions.
- `infrastructure` owns JPA, Firebase, MinIO, external commands, and framework adapters.
- API code must not access repositories or feature infrastructure directly.
- Domain code must not depend on API packages, Spring Web, Firebase, MinIO, or infrastructure implementations.
- Application code must not depend on controllers or API request DTOs.
- Extend ArchUnit when new boundaries appear.

## Java Structure

- One top-level public production type per file.
- Keep request DTOs, response DTOs, enums, and mappers as top-level types in separate files.
- Do not add nested, inner, local, or anonymous production classes.
- Do not add Lombok or MapStruct unless the repository already adopts them consistently.
- Avoid names such as `Manager`, `Helper`, `Utils`, `CommonService`, `BaseService`, `BaseController`, and `BaseRepository`.

## Controllers and DTOs

- Controllers may only map HTTP, validate/deserialise requests, extract the authenticated principal, delegate to application services, map responses, and select HTTP status/headers.
- Controllers must not contain business rules, repository calls, JPA mutation, transaction boundaries, Firebase calls, MinIO calls, object-key generation, or authorization based only on request data.
- Never return JPA entities from controllers.
- Place feature DTOs in the owning feature's API package unless the number of contracts justifies request/response subpackages.
- Do not expose Firebase UIDs, tokens, MinIO credentials, raw private object keys, stack traces, internal exception names, or private audit details.

## Transactions and Persistence

- Keep database transactions explicit and short.
- Do not perform Firebase, MinIO, FFmpeg, reverse-proxy, or other slow external I/O inside long database transactions.
- Persist server timestamps as UTC `Instant`; use an injectable `Clock` for time-dependent business decisions.
- PostgreSQL is the application database. Tests that prove persistence behavior must run against PostgreSQL/Testcontainers, not H2.
- Current baseline has manual `database/schema-v1.sql`, local `ddl-auto=update`, test `ddl-auto=create-drop`, and prod `ddl-auto=validate`.
- When Flyway is introduced, every shared schema change after the baseline must be a forward migration; do not edit applied migrations.

## Authentication and Authorization

- Firebase proves external identity only.
- Spring verifies Firebase ID tokens through `FirebaseTokenVerifier`; roles, account status, creator capability, ownership, and product permissions are backend-owned.
- Never trust roles, permissions, user IDs, or Firebase UIDs from React, Flutter, URL parameters, request bodies, or Firebase custom client data.
- Protected operations must enforce role/capability and resource-level access.
- Suspended or disabled local accounts must be rejected centrally before protected application code runs.
- Do not add a second authentication system or rely on Spring's generated default user.

## Uploads and External I/O

- MinIO is the intended object store, but upload/product media features are not implemented in the baseline.
- Future upload work must authorize before accepting data, generate object keys server-side, validate size/type/signature, reject empty files, bound probes/timeouts, compensate or record partial failures, and reconcile abandoned rows/objects.
- Never concatenate untrusted values into shell commands.

## React Event Client

- When added, use React, strict TypeScript, Vite, React Router, TanStack Query, Firebase Web SDK, Vitest, React Testing Library, and npm with one lockfile.
- Keep server state in TanStack Query and centralize API base URL, token attachment, token refresh, correlation ID extraction, API error parsing, 401 handling, and 403 handling.
- Do not introduce Redux or another global state library without proven need.

## Testing and Verification

- Use risk-based tests and assert observable behavior, not mock calls alone.
- Add regression tests for real defects.
- Do not disable or weaken tests, ArchUnit, Checkstyle, PMD, SpotBugs, Maven Enforcer, dependency analysis, lint, or TypeScript strictness.
- Backend verification from `backend/minis-backend`:

```bash
./mvnw spotless:apply
./mvnw --batch-mode --no-transfer-progress verify
```

- Windows PowerShell:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

## Phase Completion

- Do not create Git commits unless explicitly requested.
- Before finishing a phase, run the relevant verification, inspect the final diff and status, and confirm no real secret is tracked.
- Report preserved behavior, implemented functionality, files changed, migrations, API changes, authorization rules, transaction/failure behavior, tests, commands and results, manual scenarios, deferred limitations, risks, and a suggested commit message.
