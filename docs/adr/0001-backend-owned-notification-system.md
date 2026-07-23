---
adr: "0001"
title: "Backend-Owned Notification System with PostgreSQL Outbox and Firebase Cloud Messaging"
status: "accepted"
date: "2026-07-10"
decision_owner: "Amr Bani Irshid"
scope:
  - "MINIS Flutter mobile application"
  - "MINIS Spring Boot backend"
  - "PostgreSQL"
tags:
  - "notifications"
  - "push-notifications"
  - "firebase-cloud-messaging"
  - "transactional-outbox"
  - "postgresql"
supersedes: []
superseded_by: []
related_documents:
  - "../designs/notification-system-technical-design.md"
---

# ADR-0001: Backend-Owned Notification System with PostgreSQL Outbox and Firebase Cloud Messaging

## Status

**Accepted**

## Context

MINIS requires two related capabilities:

1. A durable in-app Notification Center with history and read/unread state.
2. Mobile push notifications for selected events such as follows, comments,
   replies, direct messages, moderation actions, and important account notices.

These capabilities have different responsibilities.

An external push provider transports temporary messages to device
installations. It is not an authoritative store for notification history,
business authorization, read state, deduplication, retention, or cross-device
consistency.

The backend must also avoid a dual-write failure between PostgreSQL and an
external push provider. Calling FCM independently from the business write can
produce either of these inconsistent outcomes:

- the database transaction commits but the push intention is lost;
- the push is sent but the database transaction later rolls back.

The design must preserve successful domain operations when FCM is unavailable
and must not require a message broker before product volume justifies one.

## Decision Drivers

The decision prioritizes:

- atomicity between the domain change and the durable notification intention;
- isolation of business-request latency and availability from FCM;
- durable in-app notification history;
- multi-device support;
- retryable and observable delivery;
- privacy of notification payloads;
- low MVP operational complexity;
- horizontal worker scaling at moderate volume;
- a migration path to a broker or another push provider.

## Decision

MINIS will use a **backend-owned notification architecture** with the following
properties:

1. **Spring Boot owns notification policy, recipient selection, authorization,
   deduplication, and read state.**
2. **PostgreSQL is the source of truth for in-app notifications and registered
   device installations.**
3. The originating business transaction atomically writes:
   - the domain change;
   - the in-app notification;
   - a notification outbox record.
4. A background worker claims committed outbox records and sends push messages
   asynchronously.
5. **Firebase Cloud Messaging (FCM)** is the initial push-delivery provider.
6. Push delivery is best-effort; the persisted in-app notification is
   authoritative.
7. Processing uses **at-least-once semantics** with idempotency and
   deduplication.
8. Personal notifications use registration-token targeting rather than public
   topics.
9. Private message content is not included in push payloads by default.
10. FCM-specific code is isolated behind a small `PushProvider` interface.

The selected MVP flow is:

```text
Domain operation
      |
      v
Single PostgreSQL transaction
      |
      +--> Persist domain change
      |
      +--> Persist in-app notification
      |
      +--> Persist outbox record
      |
      v
Transaction commits
      |
      v
Background worker
      |
      v
PushProvider abstraction
      |
      v
Firebase Cloud Messaging
      |
      v
Recipient device installations
```

The external FCM call is never executed inside the originating business
transaction.

## Rationale

This is the smallest design that satisfies the required correctness boundary
without adding a dedicated broker.

PostgreSQL already stores MINIS users, comments, follows, messages, and
authorization-related data. Keeping notification state in the same system
allows notification creation to participate in the same local transaction as
the domain operation.

The transactional outbox makes the intention to deliver durable. A worker can
retry after process restarts or provider outages without rolling back the
original product action.

FCM fits the existing Flutter and Firebase direction and provides official
server and client integrations. It is used only as a transport provider, not as
the notification database.

## Considered Alternatives

### Call FCM inside the business transaction — Rejected

This keeps database locks and connections open during an external network call,
adds provider latency to the request, and still cannot make PostgreSQL and FCM
commit atomically. It may send a push for a transaction that later rolls back.

### In-memory `AFTER_COMMIT` listener — Rejected

An after-commit listener fixes ordering but not durability. A process crash,
deployment, or node failure after database commit can lose the in-memory work,
and retry state is not naturally inspectable.

### PostgreSQL transactional outbox — Selected

It atomically persists the notification intention, isolates the request from
FCM, survives restarts, supports durable retries, and requires no new
infrastructure for the MVP.

### Outbox plus Kafka, RabbitMQ, or Pub/Sub — Deferred

A broker provides stronger high-volume fan-out and independent consumers, but
adds infrastructure, credentials, monitoring, and operational failure modes.
The database-to-broker boundary would still require an outbox or CDC mechanism.

### Firestore as notification history — Rejected

This creates a second application data store and a cross-database dual-write
between PostgreSQL and Firestore. FCM does not require Firestore.

### OneSignal or another engagement platform — Deferred

A customer-engagement platform becomes attractive if MINIS later needs
campaigns, segmentation, journeys, experimentation, or multi-channel
marketing. Those are not MVP requirements.

### Direct FCM plus direct APNs integration — Rejected for MVP

Maintaining separate Android and Apple provider implementations adds
credentials, error models, payload code, testing, and operational burden
without a current product requirement for that level of platform control.

## Delivery Semantics

The system guarantees:

- a committed domain operation that requires a notification has a durable
  notification and durable outbox record;
- an uncommitted or rolled-back operation has neither;
- FCM failure does not fail the originating domain operation;
- pending work survives backend restarts;
- delivery may be retried and therefore may be attempted more than once.

The system does **not** guarantee exactly-once visible display on the device.
The end-to-end path crosses PostgreSQL, worker processes, networks, FCM,
platform push services, and the operating system.

Logical effects are deduplicated using stable notification and delivery keys.
Push copy must remain harmless if the operating system displays a duplicate.

## Security and Privacy Decisions

- Firebase sending credentials exist only in trusted backend environments.
- Registration tokens are treated as secrets and redacted from logs.
- Notification APIs derive the recipient from the authenticated identity.
- A target identifier in a push payload never grants access; the backend
  re-checks authorization after the user opens it.
- Push payloads contain minimal routing metadata.
- Direct-message bodies are omitted by default because lock screens and
  connected devices may expose notification text.
- The client fetches authoritative current data from Spring Boot after a tap.

## Consequences

### Positive

- Durable Notification Center independent of push permission or delivery.
- Correct transaction boundary between domain state and notification intent.
- Core API latency and availability are isolated from FCM.
- Retry state and failures are observable.
- Multiple installations per user are supported.
- FCM can later be replaced behind an adapter.
- The design can migrate to CDC or a broker without changing the product-facing
  notification model.

### Negative

- More application code and database tables than a direct FCM call.
- The team owns token lifecycle, retries, stale-claim recovery, and provider
  error classification.
- PostgreSQL is also used as a moderate-volume work queue.
- Polling introduces a small asynchronous delay.
- At-least-once processing requires idempotent state transitions.
- Completed outbox rows require retention and vacuum/index discipline.

## Key Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Duplicate push attempts or display | Stable IDs, unique constraints, idempotent state changes, harmless copy |
| FCM outage causes retry storm | Exponential backoff, jitter, `Retry-After`, bounded retries |
| Worker crashes after claiming work | Claim timestamps and stale-claim recovery |
| Invalid or stale tokens accumulate | Token refresh upsert, last-seen tracking, permanent-error invalidation |
| Account switch sends private push to old user | Stable installation ID and atomic user/token reassociation |
| Outbox growth degrades PostgreSQL | Partial indexes, bounded batches, retention, vacuum monitoring |
| Notification volume exceeds DB queue capacity | Scale workers first; migrate relay to broker/CDC when measured thresholds are exceeded |
| Provider lock-in | Provider-neutral message model and narrow `PushProvider` adapter |

## Scope Boundary

This ADR records the architectural decision and its rationale. It intentionally
does not define final:

- table DDL and index names;
- REST request/response schemas;
- worker polling intervals and batch sizes;
- retry counts and timing constants;
- metrics and alert thresholds;
- Flutter UI behavior;
- retention periods;
- rollout tasks.

Those details are defined in:

> [`Notification System Technical Design`](../designs/notification-system-technical-design.md)

## Revisit Triggers

Create a superseding ADR when one or more of these conditions becomes true:

- PostgreSQL queue contention or outbox age regularly violates objectives;
- the platform needs several independent event consumers;
- fan-out becomes large enough to require broker-native distribution;
- MINIS adopts Kafka, RabbitMQ, Pub/Sub, or Debezium platform-wide;
- product requirements add marketing campaigns, segmentation, or journeys;
- direct APNs control or another push provider becomes necessary;
- privacy or regulatory requirements require stronger payload protection;
- Spring Modulith becomes the project-wide event publication mechanism.

## References

- Architectural Decision Records: https://adr.github.io/
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
- PostgreSQL `SELECT` and `SKIP LOCKED`:
  https://www.postgresql.org/docs/current/sql-select.html
- Spring Framework, Transaction-bound events:
  https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
