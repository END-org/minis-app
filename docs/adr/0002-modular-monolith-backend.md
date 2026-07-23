---
adr: "0002"
title: "Modular Monolith Backend with Package-by-Feature Boundaries"
status: "accepted"
date: "2026-07-13"
decision_owner: "Amr Bani Irshid"
scope:
  - "MINIS Spring Boot backend"
  - "PostgreSQL"
tags:
  - "modular-monolith"
  - "spring-boot"
  - "package-by-feature"
  - "archunit"
  - "module-boundaries"
supersedes: []
superseded_by: []
---

# ADR-0002: Modular Monolith Backend with Package-by-Feature Boundaries

## Status

**Accepted**

## Context

MINIS MVP includes several business capabilities:

- identity and users;
- creators;
- content and media;
- feed;
- likes, saves, and follows;
- comments and replies;
- notifications;
- creator-centered chat;
- moderation;
- admin operations.

The backend is initially owned and implemented by one backend engineer. The
product must move quickly while still preserving boundaries that prevent the
Spring Boot application from becoming one tightly coupled collection of global
controllers, services, repositories, and entities.

Starting with microservices would introduce:

- multiple deployments;
- network communication between business capabilities;
- distributed tracing requirements;
- service discovery or routing concerns;
- repeated authentication and authorization boundaries;
- distributed transactions and consistency problems;
- more complex testing and local development;
- operational burden that is not justified by MVP scale or team size.

A traditional package-by-layer structure would keep the deployment simple but
would mix unrelated business capabilities inside global technical folders.

The architecture needs:

- one simple deployment model;
- explicit business boundaries;
- clear ownership of data and behavior;
- low operational overhead;
- testable dependency rules;
- a future path to extract a module only when measurements and organization
  justify it.

## Decision Drivers

- one backend engineer owns the initial implementation;
- MVP delivery speed;
- low VPS operational complexity;
- strong domain separation;
- maintainability as features grow;
- ability to test module boundaries automatically;
- local ACID transactions where business operations require them;
- no premature distributed systems;
- future extractability of independently evolving modules.

## Decision

MINIS will use a **modular monolith** implemented as one Spring Boot codebase.

The backend will use:

1. one source repository inside the MINIS monorepo;
2. one primary Spring Boot application artifact;
3. one primary PostgreSQL database;
4. package-by-feature organization;
5. explicit module application interfaces;
6. logical table ownership by module;
7. direct module calls for synchronous business requirements;
8. domain or application events for secondary side effects;
9. ArchUnit tests to enforce dependency rules;
10. no microservices for the MVP.

The same application artifact may run as more than one process for workload
isolation, such as:

```text
backend API process
media worker process
notification worker process
```

Separate runtime processes do not make the system microservices when they share
the same codebase, domain model, database, release, and module boundaries.

## Package Structure

The top-level package is organized by business capability:

```text
com.minis
|
+-- identity
+-- user
+-- creator
+-- content
+-- feed
+-- social
+-- comment
+-- notification
+-- chat
+-- moderation
+-- admin
+-- media
+-- shared
```

A module may use this internal shape:

```text
feature/
|
+-- api/             Controllers and external request/response models
+-- application/     Use cases, commands, queries, transaction orchestration
+-- domain/          Domain rules, aggregates, value objects, domain events
+-- infrastructure/  Persistence, external adapters, configuration
```

This internal shape is guidance, not a requirement to create empty folders.

Classes are placed according to actual responsibilities.

## Module Boundary Rules

### Rule 1: Modules expose supported entry points

A module exposes application-level interfaces or query services.

Other modules do not call arbitrary internal classes.

### Rule 2: Repositories are module-internal

A repository belonging to one module is not injected into another module.

Incorrect:

```text
Notification module
    -> directly updates CommentRepository
```

Correct:

```text
Notification module
    -> calls supported Comment query interface when needed
```

### Rule 3: Modules own their writes

A module does not directly modify tables owned by another module.

Cross-module writes use the owning module's application interface.

### Rule 4: Shared code remains small

The `shared` package may contain genuinely cross-cutting technical primitives,
such as:

- base error contracts;
- authenticated principal;
- clock abstraction;
- cursor primitives;
- correlation IDs;
- small transaction or event infrastructure.

It must not become a dumping ground for domain services, entities, or
repositories.

### Rule 5: Cycles are prohibited

Module dependency cycles are not allowed.

Example of a problematic cycle:

```text
content -> comment -> notification -> content
```

The cycle must be broken using:

- a narrower interface;
- an event;
- a query port;
- changed ownership;
- a separate orchestration layer.

## Communication Model

Use direct application-interface calls when the caller requires:

- an immediate answer;
- validation in the current operation;
- a shared local transaction;
- a synchronous business invariant.

Use domain or application events for:

- notifications;
- analytics;
- audit side effects;
- cache invalidation;
- non-critical follow-up work;
- communication that should not create reverse dependencies.

Example:

```text
CommentApplicationService
    |
    +-- synchronously persists comment
    |
    +-- publishes CommentCreated event
              |
              +--> Notification module creates notification/outbox
              |
              +--> Analytics adapter records event when enabled
```

Events are not a replacement for normal method calls.

## Transaction Model

A use case owns its local PostgreSQL transaction.

Transactions may include writes from collaborating modules only through
supported module interfaces when atomicity is required.

External network calls are not included inside long database transactions.

Examples:

- Firebase Cloud Messaging runs after durable outbox creation;
- FFmpeg runs in a background worker;
- MinIO operations are coordinated with explicit state rather than assumed to
  participate in a PostgreSQL transaction;
- WebSocket delivery occurs after message persistence.

## Data Ownership

The MVP uses one PostgreSQL database.

Modules logically own tables:

```text
user          -> users and profiles
creator       -> creator-specific state
content       -> posts and content metadata
social        -> likes, saves, follows
comment       -> comments and replies
notification  -> notifications, devices, outbox, deliveries
chat          -> conversations, messages, read state
moderation    -> reports, blocks, actions, audit records
media         -> media objects and processing jobs
```

Logical ownership does not require a PostgreSQL schema per module.

Cross-module foreign keys may be used when they provide real integrity value,
but they do not grant repository ownership to the consuming module.

## Boundary Enforcement

ArchUnit tests will verify rules such as:

- no module accesses another module's `infrastructure` package;
- no module imports another module's internal repository implementation;
- selected domain packages do not depend on web controllers;
- module dependency cycles are absent;
- admin APIs do not bypass application services;
- notification provider classes do not leak into comment, social, or chat
  domain packages.

Example direction:

```java
noClasses()
    .that().resideOutsideOfPackage("..notification..")
    .should().dependOnClassesThat()
    .resideInAPackage("..notification.infrastructure..");
```

The exact test suite will evolve with the real package structure.

## Runtime and Deployment Model

Initial deployment uses Docker on one VPS.

Expected backend runtime processes:

```text
backend-api
media-worker
```

The notification worker may initially run inside the API process or as a
separate process depending on operational simplicity. Its architectural
behavior remains defined by ADR-0001.

All processes use the same versioned backend artifact.

Worker concurrency is bounded to protect VPS CPU, memory, temporary disk, and
database connections.

## Considered Alternatives

## Microservices — Rejected for MVP

Advantages:

- independent deployment and scaling;
- stronger runtime isolation;
- separate technology choices;
- clear network boundaries.

Reasons for rejection:

- one backend engineer;
- higher operational cost;
- distributed failure modes;
- more complex local development;
- no demonstrated independent scaling requirement;
- distributed consistency would complicate notifications, comments, chat, and
  moderation;
- one VPS does not benefit from pretending local modules are remote services.

## Traditional Package-by-Layer Monolith — Rejected

Example:

```text
controller/
service/
repository/
entity/
```

Advantages:

- familiar;
- simple for a small CRUD application.

Reasons for rejection:

- feature boundaries disappear;
- changes to one capability spread across global folders;
- internal APIs are unclear;
- repositories become globally injectable;
- cyclic dependencies become easier;
- extracting or testing one capability becomes harder.

## Maven Multi-Module Build — Deferred

Advantages:

- compile-time module boundaries;
- separate dependency declarations;
- clearer build ownership.

Reasons for deferral:

- additional build complexity before the package structure stabilizes;
- one team and one release artifact;
- ArchUnit plus package visibility is sufficient initially.

Reconsider when module size, build time, ownership, or dependency isolation
demands stronger compilation boundaries.

## Spring Modulith — Deferred

Advantages:

- Spring-aware module model;
- module verification;
- application events and documentation support.

Reasons for deferral:

- MINIS does not currently require another architecture framework;
- clear package boundaries and ArchUnit provide the needed initial control;
- event persistence requirements are already explicitly designed for
  notifications.

Reconsider if Spring Modulith capabilities provide concrete value across
several modules.

## Consequences

### Positive

- one deployable backend architecture for the MVP;
- low operational overhead;
- local transactions remain available;
- business capabilities remain discoverable;
- module boundaries can be tested;
- code ownership is clearer;
- side effects can be decoupled without a broker;
- future module extraction remains possible.

### Negative

- one bad in-process dependency can still violate boundaries unless tests catch
  it;
- one deployment failure can affect the entire backend;
- modules share JVM memory, CPU, and database resources;
- independent module deployment is unavailable;
- developers must actively protect the `shared` package;
- database ownership remains a convention enforced by code and review rather
  than separate database credentials.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Monolith becomes tightly coupled | Package-by-feature, explicit interfaces, ArchUnit |
| Shared package becomes dumping ground | Restrict allowed contents and review dependencies |
| Module dependency cycles | ArchUnit cycle checks and events/query ports |
| Worker CPU harms API latency | Separate worker process and bounded concurrency |
| Direct database access bypasses owner | Keep repositories package-internal and test dependencies |
| Too many synchronous cross-module calls | Use events for side effects and redesign ownership |
| Premature abstraction | Expose only interfaces required by real use cases |

## Implementation Constraints

- Do not create empty package trees for every module.
- Do not expose all services as public by default.
- Do not inject repositories across feature boundaries.
- Do not use events when the caller requires an immediate result.
- Do not use a broker only to simulate module separation.
- Do not introduce microservices without a superseding ADR.
- Do not duplicate domain entities in `shared`.

## Revisit Triggers

Reconsider this ADR when:

- separate teams own modules independently;
- one module requires materially different scaling;
- one capability requires a different availability boundary;
- deployment frequency differs significantly by module;
- a module requires a different data store for measured reasons;
- build times or dependency control justify Maven modules;
- a broker/event platform becomes a platform-wide dependency;
- one module can be extracted without distributed transaction coupling;
- regulatory or security constraints require runtime isolation.

A future ADR must supersede this decision rather than silently changing the
backend structure.

## References

- ArchUnit User Guide:
  https://www.archunit.org/userguide/html/000_Index.html
- ArchUnit use cases:
  https://www.archunit.org/use-cases
