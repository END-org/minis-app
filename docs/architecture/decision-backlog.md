# MINIS Architecture Decision Backlog

**Status:** Active  
**Owner:** Amr Bani Irshid  
**Last Updated:** 2026-07-13

## 1. Purpose

This file tracks architecturally significant questions that are:

- already decided and documented;
- directionally decided but still need a dedicated ADR or technical design;
- not decided yet;
- implementation details that should not become ADRs.

It is a planning index, not an ADR.

A decision should receive an ADR when it:

- changes system structure or deployment boundaries;
- creates meaningful long-term coupling;
- has multiple credible alternatives;
- affects important quality attributes;
- would be expensive to reverse later.

Small implementation choices do not need individual ADRs.

## 2. Accepted Decisions

## ADR-0001: Backend-Owned Notification System

**Status:** Accepted

Decision:

```text
Spring Boot owns notification policy and authorization.
PostgreSQL stores durable in-app notifications.
A PostgreSQL transactional outbox stores push-delivery intent.
A background worker dispatches through Firebase Cloud Messaging.
Push is best-effort.
In-app notification state is authoritative.
```

Documents:

- `../adr/0001-backend-owned-notification-system.md`
- `../designs/notification-system-technical-design.md`

## ADR-0002: Modular Monolith Backend

**Status:** Accepted

Decision:

```text
One Spring Boot codebase
One primary PostgreSQL database
Package by business feature
Logical table ownership per module
Direct calls for synchronous requirements
Events for secondary side effects
ArchUnit boundary tests
No MVP microservices
```

Document:

- `../adr/0002-modular-monolith-backend.md`

## ADR-0003: Firebase Authentication Integration

**Status:** Accepted

Decision:

```text
Flutter authenticates with Firebase.
Spring verifies Firebase ID tokens using Firebase Admin SDK.
MINIS users use an internal UUID.
firebase_uid is a unique external identity.
PostgreSQL owns roles and account status.
An explicit idempotent bootstrap endpoint creates/synchronizes the local user.
Email/password accounts require verified email.
Secrets remain outside Git.
Local tests use the Auth Emulator and a fake token verifier.
```

Document:

- `../adr/0003-firebase-authentication-integration.md`

## ADR-0004: Event Media Read Path

**Status:** Accepted

Decision:

```text
Curated event videos are imported through an explicit command mode.
ffprobe inspects but never modifies media.
Only the event MP4/H.264/AAC vertical profile is supported.
MinIO stores media objects.
PostgreSQL stores metadata.
Spring Boot generates short-lived signed read URLs at response time.
The demo uses a browser-reachable MinIO origin instead of rewriting signed URLs.
Live upload and media processing remain deferred.
```

Document:

- `../adr/0004-event-media-read-path.md`

## 3. Direction Selected, Dedicated ADR Still Required

These items have an agreed direction, but important operational and
implementation details still need a separate ADR or technical design before
the corresponding feature is implemented.

## 3.1 Live Media Upload Strategy

**Current direction:**

- MinIO is the initial S3-compatible object store.
- Initial deployment is on the project VPS.
- Application metadata remains in PostgreSQL.
- Binary media is not stored in PostgreSQL.
- Phase 4 decided the curated read path only; live upload remains undecided.

**ADR needed:** Yes

**Questions to finalize:**

- Does Flutter upload through Spring Boot or use presigned MinIO URLs?
- How are upload sessions authorized and expired?
- How are object keys structured?
- Which MIME types, extensions, file signatures, and size limits are accepted?
- How is upload completion verified?
- How are orphaned objects cleaned?
- How are private and public buckets separated?
- What is the off-VPS backup and restore strategy?
- When should a CDN be introduced?

**Proposed ADR:**

```text
ADR-0005: Live Media Upload Strategy
```

## 3.2 Media Processing Strategy

**Current direction:**

- Use FFprobe to inspect media.
- Use FFmpeg for thumbnail generation and bounded transformations.
- Run processing asynchronously.
- Run the media worker as a separate Docker process from the same Spring Boot
  codebase.
- Store durable job state in PostgreSQL.
- Keep worker concurrency bounded on the VPS.
- Do not implement HLS, DASH, adaptive bitrate ladders, or multiple renditions
  for the MVP.

**ADR needed:** Yes

**Questions to finalize:**

- What are the exact content states and job states?
- How are jobs claimed safely?
- What are retry limits and timeouts?
- What CPU, memory, disk, and concurrency limits are enforced?
- Which codecs and containers are accepted?
- Is transcoding required or only validation plus thumbnail generation?
- How are temporary files isolated and deleted?
- How are malicious or malformed files handled?
- How is worker health observed?

**Proposed ADR:**

```text
ADR-0006: Asynchronous FFmpeg Media Processing
```

A separate technical design should follow the ADR.

## 3.3 Chat Realtime Transport

**Current direction:**

- Use WebSocket for active chat delivery.
- Persist messages before acknowledging durable success.
- PostgreSQL remains the message source of truth.
- Reconnect or refresh recovers missed messages.
- Push notification is used when appropriate for inactive recipients.

**ADR needed:** Yes

**Questions to finalize:**

- Raw WebSocket or STOMP over WebSocket?
- Are message writes sent over WebSocket or normal HTTP?
- How is each connection authenticated?
- How are token expiry and reconnect handled?
- How are user sessions mapped to connections?
- What is the application message envelope?
- How are duplicate client sends deduplicated?
- How is ordering preserved?
- How are multiple devices handled?
- How will horizontal scaling work if multiple backend instances are added?
- Is a broker needed only after scaling beyond one instance?

**Proposed ADR:**

```text
ADR-0006: WebSocket Chat Delivery
```

A separate chat technical design should follow the ADR.

## 4. Pending Decisions

## 4.1 VPS Deployment and Edge Configuration

**Question:**

How will the initial VPS deployment handle TLS, routing, container restarts,
network isolation, health checks, and zero/low-downtime updates?

**Current direction:**

- Docker-based deployment on one VPS.
- Exact reverse proxy and deployment automation are not selected.

**Possible options:**

- Caddy;
- Traefik;
- Nginx;
- direct container exposure is not recommended.

**ADR required:** Possibly. Create one if the decision also defines the
production topology, release method, or operational ownership.

## 4.2 Backup and Disaster Recovery

**Question:**

How are PostgreSQL and MinIO restored after VPS or disk loss?

**Required direction:**

Backups must leave the same VPS.

**Pending details:**

- backup destination;
- backup frequency;
- retention;
- encryption;
- restoration testing;
- acceptable recovery point objective;
- acceptable recovery time objective.

**ADR required:** Yes before real production data.

## 4.3 Search Implementation

**Current direction:**

PostgreSQL search is sufficient for MVP.

**Pending details:**

- `ILIKE` versus PostgreSQL full-text search;
- language configuration;
- ranking;
- indexes;
- typo tolerance;
- search pagination.

**ADR required:** Not initially. Create one only if a separate search engine or
significant indexing architecture is introduced.

## 4.4 Rate Limiting

**Question:**

How will MINIS enforce rate limits for:

- authentication bootstrap abuse;
- comments and replies;
- follows;
- reports;
- chat messages;
- upload initiation;
- admin operations?

**Possible direction:**

- application-level Bucket4j;
- reverse-proxy limits;
- PostgreSQL-backed limits;
- Redis only when distributed coordination becomes necessary.

**ADR required:** Only if shared distributed rate-limiting infrastructure is
introduced.

## 4.5 Analytics Collection

**Question:**

How will product events such as playback, completion, notification opening, and
messaging activity be collected?

**Constraints:**

- message text must not enter general analytics;
- analytics failure must not fail core product operations;
- event volume must not overload PostgreSQL.

**ADR required:** Yes before selecting an external analytics/event platform.

## 4.6 Admin Frontend Architecture

**Question:**

Which web framework and deployment method will be used for `web/admin`?

**ADR required:** Usually no, unless the choice creates a major platform
dependency or changes repository/deployment architecture.

## 4.7 Media Delivery and CDN

**Question:**

When does direct MinIO delivery become insufficient?

**Revisit signals:**

- high egress load on the VPS;
- playback startup latency;
- geographic distribution;
- need for signed CDN URLs;
- multiple video renditions;
- cache invalidation requirements.

**ADR required:** Yes when a CDN or managed media platform is introduced.

## 4.8 Database Connection Pool and Resource Budgets

**Question:**

How should API and worker processes divide:

- PostgreSQL connections;
- CPU;
- memory;
- temporary disk;
- media-processing concurrency?

**ADR required:** No. This belongs in deployment and performance configuration
unless it changes the architecture.

## 4.9 Account Deletion and Data Retention

**Question:**

How are users, content, comments, notifications, conversations, messages,
reports, and audit records treated when account deletion is implemented?

**ADR required:** Potentially, but a product/privacy policy decision is required
first.

## 5. Technical Designs Expected Later

Create these only when implementation is close enough that the open questions
can be answered concretely:

```text
docs/designs/
|
+-- media-upload-technical-design.md
+-- media-processing-technical-design.md
+-- chat-system-technical-design.md
+-- authentication-bootstrap-technical-design.md   optional
```

Do not create empty design documents only to fill the directory.

## 6. Decisions That Do Not Need Separate ADRs Now

The following belong in code conventions, technical designs, OpenAPI,
migrations, or configuration:

- controller naming;
- DTO suffixes;
- exact endpoint paths;
- exact table and index names;
- mapper choice;
- validation annotation choice;
- worker polling interval;
- thread-pool sizes;
- pagination default limit;
- exception class names;
- individual dependency versions;
- Docker Compose service names;
- log message wording.

## 7. Next Recommended Decision Order

```text
1. Implement ADR-0002 module structure.
2. Implement ADR-0003 authentication bootstrap vertical slice.
3. Create ADR-0004 before implementing real media upload.
4. Create ADR-0005 before running FFmpeg in production.
5. Create ADR-0006 when beginning chat implementation.
6. Define backup and restore before production launch.
```

## 8. Backlog Maintenance Rule

When a decision is finalized:

1. create the ADR;
2. mark its status `accepted`;
3. move the item into Accepted Decisions;
4. link it from `system-overview.md`;
5. create a technical design only if implementation requires substantial detail;
6. supersede rather than silently rewrite old architectural decisions.
