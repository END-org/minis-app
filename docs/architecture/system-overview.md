# MINIS System Architecture Overview

**Status:** Active  
**Owner:** Amr Bani Irshid  
**Last Updated:** 2026-07-13

## 1. Purpose

This document provides the current high-level technical architecture of MINIS.

It explains:

- the applications that make up the system;
- the main backend business modules;
- the authoritative data stores;
- the external platform dependencies;
- the primary communication paths;
- the current deployment direction;
- the boundaries between architectural overview, ADRs, and technical designs.

This document intentionally avoids final endpoint schemas, complete table
definitions, class-level design, retry constants, and infrastructure tuning.
Those details belong in feature technical designs, OpenAPI, Flyway migrations,
and deployment configuration.

## 2. Product Scope

MINIS is a mobile-first mini-films platform.

The MVP includes:

- guest browsing;
- Firebase-based authentication;
- Viewer, Creator, and Admin roles;
- video and image publishing by approved creators;
- latest-ordered feed;
- likes, saves, follows, comments, and replies;
- search and discovery;
- reporting and moderation;
- durable in-app and push notifications;
- creator-centered one-to-one text chat;
- a separate admin web application.

The Product Requirements Document remains the source of truth for product scope
and MVP behavior.

## 3. Repository Strategy

MINIS uses a monorepo.

The intended repository shape is:

```text
minis-app/
|
+-- mobile/                  Flutter mobile application
+-- backend/                 Spring Boot backend
+-- web/
|   +-- admin/               Admin web application
+-- infrastructure/         Docker and deployment configuration
+-- docs/
    +-- prd/
    +-- architecture/
    +-- adr/
    +-- designs/
    +-- api/
    +-- database/
```

Exact folder names may follow the existing repository conventions, but the
applications and documentation remain in one repository.

## 4. High-Level System Context

```text
+--------------------------------------------------------------+
|                         MINIS Users                          |
|--------------------------------------------------------------|
| Guests | Viewers | Creators | Admins                         |
+-----------------------------+--------------------------------+
                              |
                              v
+-----------------------------+--------------------------------+
| Client Applications                                          |
|--------------------------------------------------------------|
| Flutter Mobile Application | Admin Web Application           |
+-----------------------------+--------------------------------+
                              |
                              | HTTPS APIs
                              | WebSocket for active chat
                              v
+--------------------------------------------------------------+
| Spring Boot Backend                                          |
|--------------------------------------------------------------|
| Identity | Users | Creators | Content | Feed | Social        |
| Comments | Notifications | Chat | Moderation | Admin         |
| Media orchestration | Shared infrastructure                  |
+------------+--------------------------+----------------------+
             |                          |
             | SQL                      | S3-compatible API
             v                          v
+--------------------------+   +-------------------------------+
| PostgreSQL               |   | MinIO                         |
|--------------------------|   |-------------------------------|
| Application data         |   | Videos                       |
| Notification outbox      |   | Images                       |
| Chat history             |   | Thumbnails                   |
| Processing jobs          |   | Avatars                      |
| Audit records            |   | Generated media              |
+--------------------------+   +-------------------------------+

External platform dependencies:

+-----------------------------+
| Firebase Authentication     |
| Firebase Cloud Messaging    |
+-----------------------------+
```

## 5. Applications

## 5.1 Flutter Mobile Application

The Flutter application provides:

- guest feed browsing;
- Firebase sign-in flows;
- feed playback;
- creator profiles;
- likes, saves, follows, comments, and replies;
- upload flows for approved creators;
- Notification Center;
- push-token registration;
- conversation list and chat room;
- profile and account settings.

The mobile client is not trusted for:

- role assignment;
- authorization;
- ownership checks;
- notification recipient selection;
- moderation decisions;
- media publication state;
- chat participant validation.

## 5.2 Spring Boot Backend

Spring Boot is the authoritative application layer.

It owns:

- mapping Firebase identities to MINIS users;
- local account status and roles;
- creator permissions;
- content metadata and lifecycle;
- feed queries;
- social relationships;
- comments and replies;
- notification persistence and push orchestration;
- conversations and messages;
- reports, blocking, moderation, and audit records;
- admin operations;
- media-upload orchestration and processing-job state.

The backend is implemented as a modular monolith.

## 5.3 Admin Web Application

The admin web application consumes protected Spring Boot APIs.

It supports:

- user and creator management;
- content and comment removal;
- report review;
- limited reported-message investigation;
- messaging suspension;
- important account notices;
- basic analytics.

The admin application does not bypass backend authorization and does not connect
directly to PostgreSQL.

## 6. Backend Architecture Style

MINIS uses one Spring Boot codebase organized as a modular monolith.

The code is organized by business capability:

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

Each feature package may contain its own:

```text
api/
application/
domain/
infrastructure/
```

The project does not use one global `controller`, `service`, `repository`, and
`entity` package for all features.

Architectural boundaries are documented and checked with ArchUnit tests.

## 7. Module Ownership

## 7.1 Identity Module

Owns:

- Firebase ID-token verification integration;
- authenticated principal creation;
- bootstrap of the local MINIS account;
- identity-to-user mapping.

Firebase proves external identity. It does not own MINIS roles or application
authorization.

## 7.2 User Module

Owns:

- local user records;
- profile data;
- account status;
- role assignments;
- profile editing.

## 7.3 Creator Module

Owns:

- creator status;
- creator permissions;
- creator public profile;
- creator-specific profile data.

## 7.4 Content Module

Owns:

- video and image posts;
- content metadata;
- content lifecycle;
- visibility and removal state;
- relationships to stored media objects.

## 7.5 Feed Module

Owns:

- latest feed ordering;
- cursor pagination;
- filtering hidden or removed content;
- feed response composition.

The Feed module reads other module data through supported module interfaces or
approved read models. It does not mutate tables owned by another module.

## 7.6 Social Module

Owns:

- likes;
- saves;
- follows;
- uniqueness and idempotency rules for those relationships.

## 7.7 Comment Module

Owns:

- comments;
- replies;
- comment ownership;
- comment deletion rules;
- comment-related domain events.

## 7.8 Notification Module

Owns:

- durable in-app notifications;
- read and unread state;
- device installations and FCM tokens;
- notification outbox;
- push-delivery attempts;
- notification deduplication.

Related documents:

- `../adr/0001-backend-owned-notification-system.md`
- `../designs/notification-system-technical-design.md`

## 7.9 Chat Module

Owns:

- one-to-one viewer-to-creator conversations;
- text messages;
- participant authorization;
- deterministic ordering;
- idempotent message creation;
- read state and unread counts;
- WebSocket delivery to active clients.

PostgreSQL remains the source of truth for accepted messages. WebSocket is a
delivery mechanism, not the durable message store.

A dedicated chat ADR and technical design will define connection management,
message protocol, reconnect behavior, authorization, and future horizontal
scaling.

## 7.10 Moderation Module

Owns:

- reports;
- user blocking;
- content and message moderation actions;
- messaging suspension;
- auditable admin actions;
- controlled access to reported-message context.

## 7.11 Media Module

Owns:

- upload orchestration;
- MinIO object identifiers;
- processing-job state;
- validation results;
- thumbnails and generated media metadata;
- publication readiness.

The initial processing direction uses FFprobe and FFmpeg through a background
media worker.

## 8. Internal Module Communication

The default communication model is:

```text
Synchronous business requirement
    -> direct call through an exposed module application interface

Asynchronous or secondary side effect
    -> domain/application event

External delivery requiring durability
    -> transactional outbox or durable job record
```

Examples:

```text
Create comment
    -> synchronous Comment application service

Comment created
    -> event consumed by Notification policy

Upload completed
    -> durable media-processing job

Message stored
    -> commit message first
    -> notify active WebSocket session
    -> create notification when required
```

Events are not used for every internal method call. Direct calls remain
appropriate when the caller requires an immediate result in the same operation.

## 9. Data Ownership

PostgreSQL is the primary application database.

Each backend module logically owns its tables.

Examples:

```text
user module          -> users and profile-related tables
content module       -> content and content metadata
comment module       -> comments and replies
notification module  -> notifications, devices, outbox, deliveries
chat module          -> conversations, participants, messages, read state
moderation module    -> reports, blocks, moderation actions, audit records
media module         -> media objects and processing jobs
```

Rules:

- a module does not directly modify tables owned by another module;
- repositories are not treated as globally shared services;
- cross-module writes go through the owning module's application interface;
- cross-module reads use supported interfaces, query services, or approved read
  models;
- Flyway migrations are the source of truth for the physical schema.

The MVP uses one PostgreSQL database. It does not use one database per module.

## 10. Authentication and Authorization

The authentication flow is:

```text
Flutter authenticates with Firebase
        |
        v
Flutter obtains Firebase ID token
        |
        v
Authorization: Bearer <Firebase ID token>
        |
        v
Spring Security verifies token using Firebase Admin SDK
        |
        v
Backend resolves firebase_uid to internal MINIS user
        |
        v
Backend checks local account status and PostgreSQL roles
        |
        v
Request continues with MINIS authenticated principal
```

Important boundaries:

- Firebase Authentication verifies external identity.
- PostgreSQL owns `VIEWER`, `CREATOR`, and `ADMIN`.
- PostgreSQL owns `ACTIVE`, `SUSPENDED`, and `DISABLED`.
- The Flutter client cannot grant itself a role.
- A valid Firebase account does not override a suspended MINIS account.
- Email/password accounts require verified email before protected product use.
- Google and Apple sign-ins use Firebase's federated identity verification.

Related ADR:

- `../adr/0003-firebase-authentication-integration.md`

## 11. Notifications

The notification architecture is:

```text
Domain transaction
    |
    +-- domain change
    +-- in-app notification
    +-- notification outbox
    |
    v
Commit
    |
    v
Notification worker
    |
    v
Firebase Cloud Messaging
```

Push delivery is best-effort. The PostgreSQL notification record is
authoritative.

Related documents:

- `../adr/0001-backend-owned-notification-system.md`
- `../designs/notification-system-technical-design.md`

## 12. Chat Direction

The selected realtime direction is WebSocket.

```text
Send message
    |
    v
Spring Boot validates participant and block state
    |
    v
Message is persisted in PostgreSQL
    |
    v
Backend acknowledges durable storage
    |
    +--> deliver over WebSocket when recipient is connected
    |
    +--> create NEW_MESSAGE notification when appropriate
```

WebSocket does not replace:

- message persistence;
- history pagination;
- authorization;
- idempotency;
- reconnect recovery;
- unread state.

The exact WebSocket protocol and scaling strategy remain for a dedicated ADR
and chat technical design.

## 13. Media Storage and Processing Direction

MinIO is the selected S3-compatible object store for the initial VPS-based MVP.

Stored objects include:

- uploaded videos;
- image posts;
- thumbnails;
- avatars;
- generated media artifacts.

Media processing does not execute inside the upload HTTP request.

The selected direction is:

```text
Creator upload
    |
    v
Backend creates upload/media record
    |
    v
Object is stored in MinIO
    |
    v
Backend creates durable processing job
    |
    v
Media worker executes FFprobe and FFmpeg
    |
    +-- inspect format, streams, duration, resolution, and codec
    +-- reject unreadable or unsupported media
    +-- generate thumbnail
    +-- perform bounded MVP transformations when required
    |
    v
Update media state to READY or FAILED
    |
    v
Content becomes publishable
```

The worker runs as a separate Docker process built from the same backend
codebase. This isolates CPU-heavy processing from HTTP request handling without
introducing a separate microservice codebase.

Advanced HLS, DASH, adaptive bitrate ladders, and multi-resolution transcoding
are outside the MVP.

A future media ADR must finalize:

- browser/mobile upload method;
- presigned-upload strategy;
- object naming;
- allowed formats and limits;
- processing-job claiming;
- retry and timeout behavior;
- backup and recovery;
- future CDN integration.

## 14. Initial Deployment Architecture

The initial production direction is one VPS using Docker.

```text
+--------------------------------------------------+
| VPS                                              |
|--------------------------------------------------|
| Docker network                                   |
|                                                  |
|  +-------------------+   +--------------------+  |
|  | Backend API       |   | Media Worker       |  |
|  | Spring Boot       |   | Spring Boot mode   |  |
|  +---------+---------+   +---------+----------+  |
|            |                       |             |
|            +-----------+-----------+             |
|                        |                         |
|            +-----------v-----------+             |
|            | PostgreSQL            |             |
|            +-----------------------+             |
|                                                  |
|            +-----------------------+             |
|            | MinIO                 |             |
|            +-----------------------+             |
|                                                  |
|  Admin web deployment and reverse proxy/TLS      |
|  details remain to be finalized.                 |
+--------------------------------------------------+

External:
- Firebase Authentication
- Firebase Cloud Messaging
- Off-VPS backup destination to be selected
```

The API and worker may use the same application image with different runtime
profiles or modes.

Example direction:

```text
APP_MODE=api
APP_MODE=media-worker
```

This remains a modular monolith because it uses one codebase, one domain model,
one primary database, and explicit internal modules. Separate runtime processes
are used for workload isolation.

## 15. Reliability Principles

- acknowledged chat messages are stored before delivery;
- push failure does not roll back the originating product action;
- media processing is asynchronous and recoverable;
- external calls do not remain inside long database transactions;
- client retries use idempotency where duplicate creation is possible;
- PostgreSQL and MinIO data require backups outside the same VPS;
- background workers use bounded concurrency;
- APIs degrade gracefully when secondary delivery systems fail.

## 16. Security Boundaries

- Firebase service credentials are not committed to Git.
- Production secrets are injected through environment variables, mounted
  secrets, or an external secret mechanism.
- MinIO credentials are backend-only.
- Clients receive only scoped upload/download access when explicitly designed.
- PostgreSQL is not exposed publicly.
- Admin APIs require backend-controlled admin roles.
- WebSocket connections require authenticated user identity.
- Message access requires conversation membership.
- Push payloads contain minimal non-sensitive routing data.
- Reported-message access is limited and auditable.

## 17. Documentation Boundaries

Use each documentation type for a different purpose:

```text
PRD
    -> what the product must do

System Overview
    -> how the whole system is currently shaped

ADR
    -> why an architecturally significant decision was made

Technical Design
    -> how one complex capability will be implemented

OpenAPI
    -> exact HTTP contract

Flyway migrations
    -> exact physical database schema
```

The System Overview may reference decisions that are already accepted and
directions that still require dedicated ADRs.

## 18. Related Documents

- `../prd/minis-prd-v1.1.md`
- `../adr/0001-backend-owned-notification-system.md`
- `../adr/0002-modular-monolith-backend.md`
- `../adr/0003-firebase-authentication-integration.md`
- `../designs/notification-system-technical-design.md`
- `decision-backlog.md`

## 19. References

- Firebase ID-token verification:
  https://firebase.google.com/docs/auth/admin/verify-id-tokens
- Firebase Authentication Emulator:
  https://firebase.google.com/docs/emulator-suite/connect_auth
- ArchUnit User Guide:
  https://www.archunit.org/userguide/html/000_Index.html
- FFmpeg documentation:
  https://ffmpeg.org/documentation.html
- FFprobe documentation:
  https://ffmpeg.org/ffprobe.html
