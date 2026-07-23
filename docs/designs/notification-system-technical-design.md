---
title: "MINIS Notification System Technical Design"
status: "active"
date: "2026-07-10"
owner: "Amr Bani Irshid"
related_adr:
  - "../adr/0001-backend-owned-notification-system.md"
systems:
  - "MINIS Flutter mobile application"
  - "MINIS Spring Boot backend"
  - "PostgreSQL"
  - "Firebase Cloud Messaging"
---

# MINIS Notification System Technical Design

## 1. Purpose

This document defines the implementation design for the notification
architecture selected in:

> [`ADR-0001: Backend-Owned Notification System with PostgreSQL Outbox and Firebase Cloud Messaging`](../adr/0001-backend-owned-notification-system.md)

The ADR records **why** the architecture was selected. This document specifies
**how** the MVP should be implemented, operated, tested, and evolved.

## 2. Goals

The implementation must provide:

- a durable in-app Notification Center;
- read/unread state shared across devices;
- push delivery for selected transactional events;
- atomic persistence of domain state, notification state, and delivery intent;
- request-path isolation from FCM latency and outages;
- durable retry and failure diagnostics;
- support for multiple installations per user;
- privacy-aware payload construction;
- horizontally scalable workers for moderate volume;
- a clear path toward a broker or another provider.

## 3. Non-Goals

The MVP does not include:

- marketing campaigns or customer journeys;
- email or SMS channels;
- exactly-once device display;
- public-topic broadcasts for private events;
- a general event-streaming platform;
- WebSocket or SSE chat transport;
- final notification copywriting and localization workflow;
- a guaranteed device-delivery SLA from FCM.

## 4. Requirements

### 4.1 Functional Requirements

Initial notification types include:

- `NEW_FOLLOWER`
- `NEW_COMMENT`
- `COMMENT_REPLY`
- `NEW_MESSAGE`
- `CONTENT_REMOVED`
- `ACCOUNT_NOTICE`

The system must allow a user to:

- list their notifications;
- retrieve an unread count;
- mark one notification as read;
- mark all notifications as read;
- register or refresh a device installation;
- disable an installation.

### 4.2 Correctness Requirements

- A rolled-back domain operation must not leave a notification or outbox record.
- A committed operation requiring a notification must leave durable delivery
  intent.
- A provider outage must not roll back a comment, follow, reply, message, or
  moderation action.
- Reprocessing the same logical domain event must not create duplicate
  notification rows.
- Account switching must not leave a token associated with the previous user.

### 4.3 Operational Requirements

Operators must be able to determine:

- whether a notification exists;
- whether its outbox record is pending, processing, retrying, succeeded, or
  dead;
- which installations were targeted;
- which attempts failed and why;
- whether an installation token was invalidated;
- the age and size of the pending queue.

## 5. Architecture Overview

```text
+---------------------------+
| Flutter Application       |
|---------------------------|
| Permission/token lifecycle|
| Foreground/background     |
| Notification Center UI    |
| Notification tap routing  |
+-------------+-------------+
              |
              | Device registration,
              | notification APIs,
              | authoritative fetch
              v
+---------------------------+
| Spring Boot Backend       |
|---------------------------|
| Domain modules            |
| Notification policy       |
| Notification/device APIs  |
| Outbox worker             |
| Firebase provider adapter |
+------+------+-------------+
       |      |
       |      +----------------------------+
       |                                   |
       v                                   v
+---------------------------+     +---------------------------+
| PostgreSQL                |     | Firebase                  |
|---------------------------|     |---------------------------|
| Domain tables             |     | Firebase Cloud Messaging |
| notifications             |     +-------------+-------------+
| user_devices              |                   |
| notification_outbox       |                   v
| push_deliveries           |          Recipient installations
+---------------------------+
```

## 6. Ownership Boundaries

### 6.1 Domain Modules

Domain modules remain responsible for:

- validating and persisting the original action;
- enforcing domain authorization;
- producing a stable event or invoking notification policy;
- exposing stable identifiers needed by the notification module.

They must not:

- construct FCM objects;
- store device tokens;
- implement retry logic;
- call an external push provider inside the business transaction.

### 6.2 Notification Module

The notification module owns:

- policy deciding whether an event produces a notification;
- recipient and actor calculation;
- self-notification suppression;
- blocked-user and visibility rules;
- durable notification creation;
- deduplication key construction;
- outbox creation;
- read/unread APIs;
- installation registration;
- provider-neutral push message construction;
- delivery-attempt state;
- retries and permanent-failure handling.

### 6.3 FCM Adapter

The FCM adapter owns:

- mapping provider-neutral messages to Firebase Admin SDK types;
- sending to an installation token;
- extracting provider message IDs;
- mapping FCM errors into internal result categories;
- no notification business decisions.

### 6.4 Flutter Client

Flutter owns:

- requesting push permission at an appropriate product moment;
- acquiring the FCM token;
- reacting to token refresh;
- maintaining a stable installation identifier;
- registering or disabling an installation;
- displaying foreground notifications;
- routing notification taps;
- fetching authoritative backend state.

## 7. End-to-End Event Flow

```text
Actor
  |
  | Perform domain action
  v
Spring Domain Service
  |
  | Begin PostgreSQL transaction
  | Persist domain change
  | Evaluate notification policy
  | Insert notification
  | Insert outbox record
  | Commit
  v
Return success to actor

Background Outbox Worker
  |
  | Claim eligible outbox rows
  | Mark rows PROCESSING
  | Commit short claim transaction
  | Load active recipient installations
  v
PushProvider / FCM
  |
  | Send one request per active installation
  | Return accepted, retryable, or permanent result
  v
PostgreSQL
  |
  | Record delivery result
  | Mark outbox SUCCEEDED, RETRY, or DEAD
  v
FCM attempts delivery to recipient device
```

## 8. Notification Creation

### 8.1 Transaction Boundary

For a domain action that requires a notification, one local PostgreSQL
transaction performs:

```text
1. Persist the domain change.
2. Persist the in-app notification.
3. Persist the outbox record.
4. Commit.
```

FCM is never called during this transaction.

Expected invariant:

```text
committed domain event requiring notification
    => committed notification + committed outbox record
```

and:

```text
rolled-back domain event
    => no notification + no outbox record
```

### 8.2 Policy Entry Point

A domain service may invoke a notification policy directly or publish a local
domain event handled inside the same transaction.

Conceptual interface:

```java
public interface NotificationPolicy<E> {

    Optional<NotificationCommand> evaluate(E event);
}
```

A command should contain provider-neutral, stable data:

```java
public record NotificationCommand(
    UUID recipientUserId,
    UUID actorUserId,
    NotificationType type,
    String subjectType,
    String subjectId,
    String targetType,
    String targetId,
    String dedupeKey,
    Map<String, String> templateArguments
) {}
```

The exact types should follow project conventions.

### 8.3 Deduplication Key

Examples:

```text
NEW_FOLLOWER:{followId}
NEW_COMMENT:{commentId}
COMMENT_REPLY:{replyId}
NEW_MESSAGE:{messageId}
CONTENT_REMOVED:{moderationActionId}:{recipientUserId}
```

A unique database constraint on `notifications.dedupe_key` prevents duplicate
logical records.

The key must represent the domain fact, not a worker attempt.

### 8.4 Policy Rules

At minimum, policies must consider:

- suppressing notification to the actor when actor equals recipient;
- blocked-user relationships;
- content visibility and ownership;
- muted conversations or notification preferences, when implemented;
- whether the event is product-significant enough for push;
- whether an in-app notification should exist even when push is disabled.

## 9. Data Model

The schema below is directional. Exact SQL types, naming conventions, foreign
key behavior, and migration syntax must follow the existing MINIS database
standards.

### 9.1 `notifications`

```text
id
recipient_user_id
actor_user_id nullable
type
subject_type nullable
subject_id nullable
target_type nullable
target_id nullable
title_key or title
body_key or body
template_arguments nullable
read_at nullable
created_at
dedupe_key
```

Rules:

- `recipient_user_id` is mandatory.
- `actor_user_id` is nullable for system notices.
- `read_at` is a timestamp rather than a boolean.
- `dedupe_key` is unique.
- references contain stable IDs, not serialized domain objects.
- public queries always constrain by the authenticated recipient.

Candidate indexes:

```sql
CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_user_id, created_at DESC, id DESC)
    WHERE read_at IS NULL;

CREATE UNIQUE INDEX uq_notifications_dedupe_key
    ON notifications (dedupe_key);
```

#### Why `(created_at, id)`?

The ID is a deterministic tie-breaker when multiple rows share the same
timestamp. Cursor pagination should compare both values.

### 9.2 `user_devices`

```text
id
installation_id
user_id
provider
provider_token
platform
push_enabled
last_seen_at
invalidated_at nullable
created_at
updated_at
```

Recommended constraints:

```text
UNIQUE (installation_id)
UNIQUE (provider, provider_token)
```

Whether a token can temporarily move between installations depends on observed
FCM behavior and existing account/session design. The upsert must safely detach
the token from an old user before attaching it to the current authenticated
user.

#### Installation ID

`installation_id` is a stable client-generated identifier for an application
installation. It is separate from the FCM token because FCM tokens can rotate.

#### Token Handling

- tokens are never returned by public read APIs;
- logs show only a short hash or suffix;
- refresh performs an idempotent upsert;
- logout disables or detaches the installation according to session policy;
- `UNREGISTERED` permanently invalidates the token;
- stale installations can be cleaned using `last_seen_at`.

### 9.3 `notification_outbox`

```text
id
notification_id
status
attempt_count
next_attempt_at
claimed_at nullable
claimed_by nullable
processed_at nullable
last_error_code nullable
last_error_message nullable
created_at
updated_at
```

Candidate states:

```text
PENDING
PROCESSING
RETRY
SUCCEEDED
DEAD
```

State meaning:

| State | Meaning |
|---|---|
| `PENDING` | Ready for the first worker attempt |
| `PROCESSING` | Claimed by one worker |
| `RETRY` | Retryable failure delayed until `next_attempt_at` |
| `SUCCEEDED` | All required dispatch processing completed |
| `DEAD` | Automated retry budget or expiry window exhausted |

Recommended uniqueness:

```text
UNIQUE (notification_id)
```

Candidate queue index:

```sql
CREATE INDEX idx_notification_outbox_ready
    ON notification_outbox (next_attempt_at, created_at, id)
    WHERE status IN ('PENDING', 'RETRY');
```

### 9.4 `push_deliveries`

A delivery row models one logical notification-to-installation delivery stream.

```text
id
notification_id
device_id
provider
status
attempt_count
provider_message_id nullable
last_error_code nullable
last_error_message nullable
last_attempt_at nullable
sent_at nullable
created_at
updated_at
```

Recommended uniqueness:

```text
UNIQUE (notification_id, device_id)
```

Candidate states:

```text
PENDING
ACCEPTED
RETRY
PERMANENT_FAILURE
SKIPPED
```

`ACCEPTED` means the provider accepted the request. It does not prove that the
operating system displayed the notification or that the user saw it.

## 10. Worker Design

### 10.1 Polling

The MVP worker uses Spring scheduling and polls PostgreSQL at a configurable
interval.

Configuration examples:

```text
notifications.worker.enabled
notifications.worker.poll-interval
notifications.worker.batch-size
notifications.worker.stale-claim-threshold
notifications.worker.max-attempts
notifications.worker.max-retry-delay
notifications.worker.message-expiry
```

Exact values are environment configuration, not domain constants.

### 10.2 Claim Query

A worker claims a bounded batch:

```sql
SELECT id
FROM notification_outbox
WHERE status IN ('PENDING', 'RETRY')
  AND next_attempt_at <= now()
ORDER BY next_attempt_at, created_at, id
FOR UPDATE SKIP LOCKED
LIMIT :batchSize;
```

Within the same short transaction it updates the rows:

```text
status = PROCESSING
claimed_at = now()
claimed_by = workerInstanceId
attempt_count = attempt_count + 1
```

Then it commits.

#### Why `SKIP LOCKED`?

Multiple workers can execute the claim query concurrently. Rows locked by one
worker are skipped by others instead of causing queue-wide lock waiting.

#### Why commit before FCM?

The worker must not hold a row lock or database connection while waiting for an
external network request. External latency is unbounded relative to a local
transaction.

### 10.3 Processing Algorithm

Conceptual algorithm:

```text
for each claimed outbox record:
    load notification

    load active installations for recipient

    if no active installation:
        mark outbox SUCCEEDED with result NO_ACTIVE_DEVICE
        continue

    for each installation:
        create-or-load delivery row
        skip terminal delivery rows
        call PushProvider outside a database transaction
        persist accepted, retryable, or permanent result

    if every delivery is terminal:
        mark outbox SUCCEEDED
    else if any delivery is retryable and budget remains:
        mark outbox RETRY with next_attempt_at
    else:
        mark outbox DEAD
```

The implementation may process one outbox item at a time or group database
updates, but it must preserve bounded transactions and clear ownership.

### 10.4 Stale Claim Recovery

A worker may crash after setting `PROCESSING`.

A recovery task finds:

```text
status = PROCESSING
claimed_at < now() - staleClaimThreshold
```

and transitions rows to `RETRY` with a diagnostic such as
`STALE_WORKER_CLAIM`.

Recovery must be idempotent and safe with concurrent workers.

### 10.5 Process Crash Windows

#### Crash before provider call

The stale claim is recovered and retried. No provider request was sent.

#### Crash after provider acceptance but before database update

The worker cannot know whether the provider accepted the request. The outbox may
be retried, causing a duplicate provider attempt.

This is why the system declares at-least-once semantics rather than
exactly-once display.

### 10.6 Fairness and Starvation

Queue ordering uses:

```text
next_attempt_at ASC, created_at ASC, id ASC
```

Retries should use delayed timestamps so repeatedly failing rows do not remain
at the head of the queue.

If one recipient has many installations or one item is expensive, a future
optimization can cap per-item work or fan out delivery tasks into separate rows.

## 11. Delivery Semantics and Idempotency

### 11.1 Guarantee

The processing contract is:

> At-least-once processing with idempotent internal effects.

### 11.2 Deduplication Layers

#### Logical Notification

`notifications.dedupe_key` prevents duplicate notification records.

#### Outbox

`UNIQUE(notification_id)` prevents duplicate logical outbox entries.

#### Delivery

`UNIQUE(notification_id, device_id)` prevents duplicate internal delivery
streams.

#### Flutter

The payload includes `notificationId`. Flutter should avoid executing the same
tap action twice and should navigate using backend state rather than replaying
payload mutations.

### 11.3 What Cannot Be Deduplicated Perfectly

If FCM accepts a request and the worker crashes before recording success, a
retry can produce a second visible push. Neither a PostgreSQL unique constraint
nor an application ID can force the device operating system to display exactly
once.

Therefore:

- notification text must be safe when repeated;
- opening the notification must be idempotent;
- marking as read must be idempotent;
- no sensitive business mutation occurs solely from receiving a push.

## 12. Device Registration Lifecycle

### 12.1 Registration

Flutter sends:

```http
PUT /api/v1/me/devices/{installationId}
```

Example request:

```json
{
  "provider": "FCM",
  "token": "provider-token",
  "platform": "ANDROID",
  "pushEnabled": true
}
```

The backend derives `user_id` from authentication.

The operation must:

1. validate the provider and platform;
2. locate the installation;
3. detach the same provider token from any stale association if required;
4. associate the installation with the authenticated user;
5. update the token and `last_seen_at`;
6. clear `invalidated_at` when a valid refreshed token is registered;
7. remain idempotent for repeated identical requests.

### 12.2 Token Refresh

Flutter listens for FCM token refresh and repeats the installation upsert.

A token refresh must update the existing installation rather than create
multiple active rows for one installation.

### 12.3 Logout and Account Switch

On logout, Flutter requests installation disablement or detachment:

```http
DELETE /api/v1/me/devices/{installationId}
```

The exact server action depends on session policy:

- detach the installation from the user;
- set `push_enabled = false`;
- invalidate the row;
- retain metadata for audit and reuse.

On account switch, the next authenticated upsert atomically associates the
installation and token with the new user.

### 12.4 Permission Disabled

The client should report `pushEnabled = false` when it knows permission is
disabled.

The in-app Notification Center remains fully functional.

### 12.5 Stale Token Cleanup

Candidates for cleanup:

- provider permanent errors;
- installations unseen beyond a configured threshold;
- explicit logout or uninstall signals when available;
- duplicate token rows discovered during upsert.

Cleanup must not remove the in-app notification record.

## 13. Push Provider Abstraction

Provider types must not leak into domain modules.

```java
public interface PushProvider {

    PushSendResult send(
        PushMessage message,
        DeviceRegistration device
    );
}
```

Provider-neutral message:

```java
public record PushMessage(
    UUID notificationId,
    NotificationType type,
    String title,
    String body,
    Map<String, String> data
) {}
```

Result model:

```java
public sealed interface PushSendResult
        permits PushSendResult.Accepted,
                PushSendResult.RetryableFailure,
                PushSendResult.PermanentFailure {

    record Accepted(String providerMessageId)
            implements PushSendResult {}

    record RetryableFailure(
        String code,
        String message,
        Duration retryAfter
    ) implements PushSendResult {}

    record PermanentFailure(
        String code,
        String message,
        boolean invalidateDevice
    ) implements PushSendResult {}
}
```

Initial adapter:

```java
public final class FirebasePushProvider implements PushProvider {
    // Map PushMessage to Firebase Admin SDK structures.
    // Map Firebase errors to internal result categories.
}
```

Keep the abstraction narrow. Do not model speculative provider features that
MINIS does not use.

## 14. FCM Targeting

Personal notifications use registration-token targeting.

For one recipient:

```text
recipient user
    -> active user_devices
    -> one send per token or a supported bounded multicast operation
```

Do not use public topics for:

- direct messages;
- comment replies;
- moderation notices;
- account notices;
- any event containing user-specific routing information.

Topics may later be evaluated for public broadcasts through a separate design.

## 15. Payload Design

Use an FCM notification payload plus minimal data metadata for ordinary social
notifications.

Example:

```json
{
  "notification": {
    "title": "New comment",
    "body": "Someone commented on your video"
  },
  "data": {
    "notificationId": "notif-123",
    "type": "NEW_COMMENT",
    "targetType": "POST",
    "targetId": "post-456"
  }
}
```

For a direct message:

```json
{
  "notification": {
    "title": "New message",
    "body": "You received a new message on MINIS"
  },
  "data": {
    "notificationId": "notif-123",
    "type": "NEW_MESSAGE",
    "targetType": "CONVERSATION",
    "targetId": "conversation-456"
  }
}
```

Default payload rules:

- no full message body;
- no registration token in application logs;
- no authorization decisions derived from payload data;
- no complete mutable domain object;
- stable IDs only;
- size kept well below provider limits;
- platform-specific options added only inside the adapter.

## 16. Tap and Foreground Behavior

### Background

The operating system may display the notification. When the user taps it,
Flutter reads the metadata and requests current state from Spring Boot.

### Foreground

Flutter receives the message in application code and decides whether to show:

- an in-app banner;
- a local notification;
- no interruption when the user is already viewing the relevant screen.

### Authoritative Fetch

After a tap, the backend may return:

- the current target;
- `404` when it was deleted;
- `403` when access is no longer allowed;
- a domain-specific unavailable response.

Flutter must handle these cases gracefully.

## 17. Retry Policy

### 17.1 Retryable Failures

Examples:

- network timeout;
- transient DNS or connection failure;
- HTTP `429`;
- HTTP `500`;
- HTTP `503`;
- FCM `UNAVAILABLE`;
- FCM `INTERNAL`.

Retry policy:

```text
delay = min(maxDelay, baseDelay * 2^attempt) + randomJitter
```

When FCM supplies `Retry-After`, it takes precedence over the locally calculated
minimum.

Retries are bounded by both:

- maximum attempt count;
- message expiry or business usefulness window.

### 17.2 Permanent Failures

Examples:

- `UNREGISTERED`;
- verified invalid registration token;
- sender/project mismatch;
- invalid credentials;
- malformed non-retryable request.

For `UNREGISTERED`, mark the delivery permanently failed and disable or remove
the device token.

`INVALID_ARGUMENT` must not automatically invalidate a token because it can
also indicate an invalid payload. Invalidation is safe only after the payload
is known to be valid and the token is identified as the invalid field.

### 17.3 Dead State

When no retry budget remains:

```text
outbox.status = DEAD
```

Retain diagnostic fields needed for support and metrics.

A dead push does not delete or hide the in-app notification.

## 18. Read and Unread Semantics

Unread:

```text
read_at IS NULL
```

Read:

```text
read_at = backend timestamp
```

Marking a notification read is idempotent:

```http
POST /api/v1/notifications/{notificationId}/read
```

Mark-all-read is also idempotent:

```http
POST /api/v1/notifications/read-all
```

Both operations constrain rows by the authenticated user.

A server timestamp avoids trusting client clock values.

## 19. API Direction

The following contracts are directional and should be aligned with existing
MINIS API conventions.

### 19.1 List Notifications

```http
GET /api/v1/notifications?limit=20&cursor=...
```

Ordering:

```text
created_at DESC, id DESC
```

Cursor contains both ordering values.

Example response:

```json
{
  "items": [
    {
      "id": "notif-123",
      "type": "NEW_COMMENT",
      "actor": {
        "id": "user-10",
        "displayName": "Example User",
        "avatarUrl": "..."
      },
      "target": {
        "type": "POST",
        "id": "post-456"
      },
      "title": "New comment",
      "body": "Someone commented on your video",
      "readAt": null,
      "createdAt": "2026-07-10T11:30:00Z"
    }
  ],
  "nextCursor": "..."
}
```

The response must not expose provider tokens or internal delivery state.

### 19.2 Unread Count

```http
GET /api/v1/notifications/unread-count
```

Example:

```json
{
  "count": 7
}
```

Begin with an indexed database count. Add caching or a maintained counter only
after measurement.

### 19.3 Mark One Read

```http
POST /api/v1/notifications/{notificationId}/read
```

Expected semantics:

- `200` or `204` when already read;
- `404` when the notification does not belong to the authenticated user.

### 19.4 Mark All Read

```http
POST /api/v1/notifications/read-all
```

The backend uses its own timestamp and updates only the authenticated user's
unread rows.

### 19.5 Register or Refresh Installation

```http
PUT /api/v1/me/devices/{installationId}
```

### 19.6 Disable Installation

```http
DELETE /api/v1/me/devices/{installationId}
```

## 20. Authorization and Privacy

### 20.1 Notification APIs

Every query or mutation includes:

```text
recipient_user_id = authenticated_user_id
```

The client never submits an arbitrary recipient for list or read operations.

### 20.2 Deep Links

A payload target is only a navigation hint. The target API performs normal
authorization and visibility checks.

### 20.3 Sensitive Content

Default private-message notifications are generic.

Product may later add a user preference for previews, but that requires a
separate privacy decision covering lock-screen behavior and device-level
settings.

### 20.4 Secrets

- service-account credentials are loaded from secret management;
- no credentials are committed to Git;
- the mobile application never receives server sending credentials;
- tokens are redacted from logs and traces;
- diagnostic errors are sanitized before persistence.

## 21. Observability

### 21.1 Metrics

Recommended metrics:

```text
notifications_created_total{type}
notification_outbox_pending
notification_outbox_oldest_age_seconds
notification_dispatch_attempts_total{provider,result}
notification_dispatch_latency_seconds
notification_retry_total{reason}
notification_dead_total{reason}
device_tokens_invalidated_total{reason}
notification_worker_batch_size
notification_worker_claim_duration_seconds
notification_worker_processing_duration_seconds
```

Useful dimensions must remain bounded. Do not use notification ID, user ID, or
token as metric labels.

### 21.2 Structured Logs

Include when approved:

- notification ID;
- outbox ID;
- delivery ID;
- worker instance ID;
- provider;
- normalized internal result;
- provider error code;
- attempt count;
- recipient user ID in an approved internal representation.

Exclude:

- full provider token;
- service-account data;
- private message content;
- entire payloads unless explicitly sanitized.

### 21.3 Alerts

Initial alert candidates:

- oldest pending outbox age exceeds objective;
- pending row count grows continuously;
- worker has stopped claiming records;
- dead-delivery rate spikes;
- FCM `429` or `503` rate spikes;
- invalid-token rate spikes;
- p95 or p99 dispatch latency exceeds objective.

Exact thresholds belong in environment-specific operations configuration.

## 22. PostgreSQL and Performance

### 22.1 Short Transactions

- domain transaction contains only local database work;
- claim transaction is short;
- provider network calls occur outside database transactions;
- result updates use short transactions.

### 22.2 Bounded Batches

Workers claim a configured maximum number of rows. They never load the complete
pending queue.

### 22.3 Index Discipline

Queue indexes should cover only active states when possible. Completed rows
must not dominate the hot queue index.

### 22.4 Retention

Successful outbox and delivery rows follow a retention policy.

Options:

- delete after a short operational window;
- archive into a cheaper table;
- retain dead and failed rows longer than successful rows.

Notifications themselves follow a separate product retention policy.

### 22.5 Vacuum and Write Churn

The outbox receives frequent updates across states. Monitor:

- table and index bloat;
- dead tuples;
- autovacuum frequency and duration;
- buffer-cache hit rate;
- queue query latency;
- lock waits.

Avoid repeatedly updating large payload columns. Prefer stable notification data
in `notifications` and compact mutable state in outbox/delivery rows.

### 22.6 Scale Path

Scale in this order:

1. tune indexes and batch size;
2. run additional worker instances;
3. use bounded provider batching where supported;
4. split delivery work if per-notification fan-out grows;
5. introduce an outbox relay or CDC;
6. publish to Kafka, RabbitMQ, or Pub/Sub;
7. scale provider consumers independently.

## 23. Failure Scenarios

| Scenario | Expected Behavior |
|---|---|
| FCM unavailable | Domain action succeeds; notification exists; outbox retries |
| Worker crashes before send | Stale claim recovery retries |
| Worker sends then crashes before recording | Possible duplicate send; internal effects remain idempotent |
| Domain transaction rolls back | No notification or outbox row |
| Token is unregistered | Delivery permanently fails; token invalidated |
| User has no active token | In-app notification remains; outbox completes as no active device |
| Push permission disabled | In-app notification remains; no repeated futile sends |
| Target deleted after push | Backend returns unavailable/not found; Flutter shows graceful state |
| Access revoked after push | Backend returns forbidden; no access granted by payload |
| Account changes on one installation | Installation is atomically reassociated |
| Several worker instances run | `SKIP LOCKED` distributes available rows |
| Retry storm during provider outage | Backoff, jitter, expiry, and alerting limit pressure |
| Database unavailable | Domain requests follow normal DB failure policy; worker pauses safely |
| Provider credentials invalid | Permanent operational failure, alert, no domain rollback |

## 24. Testing Strategy

### 24.1 Unit Tests

- notification policy recipient selection;
- self-notification suppression;
- dedupe-key construction;
- provider error classification;
- retry-delay calculation;
- payload privacy rules;
- state-transition validation.

### 24.2 Repository and Migration Tests

- unique dedupe constraint;
- one outbox per notification;
- one delivery per notification/device;
- unread partial-index query behavior;
- cursor ordering with equal timestamps;
- claim query with concurrent transactions.

### 24.3 Integration Tests

- domain change, notification, and outbox commit atomically;
- rollback removes all three;
- FCM adapter is not called inside the domain transaction;
- no active devices completes correctly;
- `UNREGISTERED` invalidates device;
- retryable error schedules delayed retry;
- maximum attempts transitions to `DEAD`;
- stale claims recover.

### 24.4 Concurrency Tests

Run multiple worker instances against the same database and verify:

- available rows are normally claimed once at a time;
- no queue-wide blocking from locked rows;
- stale recovery does not steal a currently active claim;
- terminal delivery state is not overwritten by a late worker;
- state updates use version checks or expected-state predicates.

Recommended update pattern:

```sql
UPDATE notification_outbox
SET status = :newStatus, ...
WHERE id = :id
  AND status = :expectedStatus
  AND claimed_by = :workerId;
```

Check the affected row count to detect stale ownership.

### 24.5 Failure-Injection Tests

- provider timeout;
- HTTP `429` with `Retry-After`;
- HTTP `503`;
- invalid token;
- invalid credentials;
- process kill after claim;
- process kill after provider acceptance;
- database failure during result recording;
- token refresh during delivery;
- account switch during pending work.

### 24.6 Flutter Tests

- permission granted and denied;
- initial token registration;
- token refresh;
- foreground message behavior;
- background tap routing;
- duplicate notification ID handling;
- deleted or forbidden target;
- logout and account switch;
- notification read synchronization.

## 25. Rollout Plan

### Phase 1: Persistence and APIs

- create `notifications`;
- implement list, unread count, mark-read, and mark-all-read;
- create `user_devices`;
- implement installation upsert and disablement;
- add constraints and indexes.

### Phase 2: Transactional Creation

- create `notification_outbox`;
- integrate one event type end to end, preferably comments or replies;
- verify atomic commit and rollback;
- add dedupe keys and policy tests.

### Phase 3: Worker and FCM

- implement claiming and stale recovery;
- implement `PushProvider`;
- add Firebase Admin SDK adapter;
- implement delivery state and retries;
- add token invalidation;
- add metrics and structured logs.

### Phase 4: Flutter

- permission flow;
- stable installation ID;
- token lifecycle;
- foreground/background handlers;
- tap routing;
- Notification Center UI;
- read synchronization.

### Phase 5: Expand Event Types

- follows;
- replies;
- direct messages;
- moderation notices;
- account notices.

Each event type should be enabled behind configuration or a feature flag when
practical.

### Phase 6: Reliability Validation

- concurrent workers;
- provider outage;
- process crashes;
- queue backlog;
- account switching;
- privacy review;
- operational dashboard and alerts.

## 26. Deployment and Migration Safety

- database migrations deploy before code that writes new tables;
- worker remains disabled until schema and provider credentials are verified;
- application code can write notifications/outbox before worker activation;
- enabling the worker processes the accumulated durable backlog;
- rollback must avoid deleting pending rows;
- new notification types should be backward-compatible with older clients;
- unknown client notification types should open the Notification Center or use
  a safe fallback route.

## 27. Configuration and Open Values

The architecture is fixed, but these values remain configurable and should be
chosen through testing:

- polling interval;
- batch size;
- stale-claim threshold;
- base retry delay;
- maximum retry delay;
- jitter range;
- maximum attempts;
- message expiry window;
- completed-row retention;
- stale-token retention;
- worker concurrency;
- maximum active installations per user;
- notification page-size limits;
- operational alert thresholds.

## 28. Acceptance Criteria

The MVP is complete when:

1. Domain change, notification, and outbox commit atomically.
2. FCM is not called inside the originating business transaction.
3. Provider failure does not fail comment, follow, reply, message, or
   moderation persistence.
4. Notification history works without push permission.
5. Read state is shared across devices after refresh.
6. One user can register multiple installations.
7. Token refresh updates the same installation.
8. Account switching removes the old user association.
9. Permanent invalid-token errors disable the registration.
10. Retryable failures use delayed backoff and jitter.
11. Duplicate domain-event processing does not duplicate notifications.
12. Several workers can claim different rows concurrently.
13. Worker crashes do not permanently strand `PROCESSING` rows.
14. Push payloads contain minimal routing data.
15. Private message text is absent by default.
16. Target APIs re-check current authorization.
17. Metrics expose pending age, retries, permanent failures, and dead rows.
18. Provider-specific classes do not appear in comment, follow, chat, or
    moderation domain code.
19. Completed outbox rows have a retention mechanism.
20. Operational documentation explains how to inspect and recover the queue.

## 29. Future Evolution

### Stage 1 — MVP

```text
PostgreSQL outbox
    -> Spring worker
    -> FCM
```

### Stage 2 — Higher Worker Throughput

```text
PostgreSQL outbox
    -> multiple workers
    -> bounded provider batching
    -> FCM
```

### Stage 3 — Event Platform

```text
PostgreSQL outbox
    -> relay or Debezium CDC
    -> Kafka / RabbitMQ / Pub/Sub
    -> notification consumers
    -> FCM
```

### Stage 4 — Multiple Providers

```text
Notification module
    -> PushProvider
       -> FCM
       -> OneSignal
       -> SNS
       -> another provider
```

The persisted notification model and public APIs should remain stable through
these stages.

## 30. References

- ADR-0001:
  `../adr/0001-backend-owned-notification-system.md`
- AWS Prescriptive Guidance, Transactional Outbox:
  https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html
- Firebase, Server environment and FCM:
  https://firebase.google.com/docs/cloud-messaging/server-environment
- Firebase, Message types:
  https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type
- Firebase, Registration token management:
  https://firebase.google.com/docs/cloud-messaging/manage-tokens
- Firebase, Error codes:
  https://firebase.google.com/docs/cloud-messaging/error-codes
- Firebase, Sending at scale:
  https://firebase.google.com/docs/cloud-messaging/scale-fcm
- Firebase, Topic messaging:
  https://firebase.google.com/docs/cloud-messaging/topic-messaging
- PostgreSQL `SELECT` and `SKIP LOCKED`:
  https://www.postgresql.org/docs/current/sql-select.html
- PostgreSQL `LIMIT` and deterministic ordering:
  https://www.postgresql.org/docs/current/queries-limit.html
- Spring Framework, Scheduling:
  https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- Spring Framework, Transaction-bound events:
  https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
