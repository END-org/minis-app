# MINIS Implemented Architecture

This document records the implemented repository structure. Product directions that are not yet
implemented remain in the PRD and decision backlog; they are not part of the current runtime.

## Runtime Shape

MINIS is a modular monolith. One Spring Boot application owns HTTP APIs, authorization, application
workflows, PostgreSQL persistence, Firebase identity verification, MinIO media storage, and bounded
`ffprobe` inspection. The React event application is the current product client. PostgreSQL and
MinIO run locally through `compose.local.yml`; the demo topology adds Spring Boot and NGINX through
`compose.demo.yml`.

No module has a separate deployment, database, queue, cache, or search service.

## Backend Packages

The Java root is `com.end.minisbackend`. Top-level packages are business capabilities:

```text
admin/       event-admin user, creator-role, status, verification, and audit use cases
content/     upload, import, read, social interaction, removal, and reconciliation
creator/     public creator profiles and creator lookup
identity/    Firebase verification, principals, and bootstrap coordination
user/        backend-owned accounts and profile updates
shared/      errors, correlation IDs, security plumbing, and technical configuration
```

Each feature uses only the layers it needs:

```text
api/             controllers and HTTP request/response contracts
application/     use cases, authorization coordination, transactions, and ports
domain/          state, invariants, policies, and transitions
infrastructure/  JPA/JDBC, Firebase, MinIO, command execution, and configuration adapters
```

The feature packages use a consistent navigation policy without forcing empty folders:

```text
api/             controller/ for REST endpoints, dto/ for HTTP contracts, mapper/ for response mapping
application/     service/ for use cases, port/ for external contracts, model/ for use-case values
domain/          model/ for state, enums/ for domain choices, policy/ for pure business rules
infrastructure/  repository/, config/, storage/, media/, firebase/, security/, and other adapters
```

`content` is grouped further by capability: its DTOs separate feed, upload, and administration;
its services separate feed, upload, import, interaction, reconciliation, and administration; and
its application models mirror those workflows. `creator`, `user`, `identity`, and `admin` use the
same layer vocabulary at a scale appropriate to each capability. Package-private state and JDBC
row mappers remain co-located with their consumers when splitting them would expose an internal
type only to satisfy directory symmetry.

The admin feature exposes separate application services for user queries, creator-role changes,
account-status changes, and creator verification. Shared admin authorization and audit behavior stay
in focused collaborators. Controllers delegate directly to these use cases.

## Dependency Direction

Dependencies point inward:

```text
api -> application -> domain
infrastructure -> application/domain
```

Application ports describe external capabilities; infrastructure classes implement them. For
example, `FirebaseWebConfigProvider` is the identity application interface and
`FirebaseWebConfigProperties` is its configuration-backed infrastructure implementation.
`ContentImportManifestReader` is the content application contract, while
`JacksonContentImportManifestReader` owns JSON parsing and filesystem validation.

Feature infrastructure is private to its owner. Cross-feature work uses application interfaces or
explicit query contracts. API contracts do not contain JPA entities, and neither API nor application
code imports infrastructure implementations.

## Persistence And Transactions

Flyway migrations V1 through V6 are the schema source of truth, and Hibernate validates that schema.
Content writes, interaction writes, upload lifecycle persistence, and published-content projections
use separate adapters. PostgreSQL/Testcontainers tests retain the production database behavior.

Upload and import workflows keep slow file, `ffprobe`, and MinIO work outside database transactions.
Reservation, publication, failure recording, and reconciliation use short explicit transactions.
Compensation deletes only newly stored objects when the persisted state does not own them.

## Frontend Structure

`web/event-app/src` is organized by application shell, core infrastructure, and product feature:

```text
app/       routes, providers, route integration tests, and focused test support
core/      typed API client and Firebase authentication session
features/  account, admin, auth, creators, feed, social collections, and uploads
shared/    reusable presentation components and foundational styles
test/      genuinely shared render and Firebase-user fixtures
```

Context definitions and hooks are separated from provider components so Fast Refresh boundaries are
clean. Server state remains in TanStack Query. Feature CSS lives with feed/content and upload
ownership; `shared/styles/global.css` contains only the common foundation and layout.

## Enforcement

`ArchitectureTest` enforces shared isolation, controller/DTO/entity/configuration/repository placement,
API/application/domain dependency direction, feature-infrastructure privacy, and top-level feature
cycle freedom. Test packages mirror production layers when doing so does not require widening
package-private test support. Maven verification also runs Spotless, Checkstyle, PMD, SpotBugs,
Enforcer, dependency analysis, unit tests, and PostgreSQL/MinIO integration tests.

See `authentication.md`, `media-and-upload.md`, `api-handoff.md`, `getting-started.md`, and
`deployment.md` for behavior and operational contracts.
