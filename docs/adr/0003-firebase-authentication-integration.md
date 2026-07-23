---
adr: "0003"
title: "Firebase Authentication Integration with Backend-Owned Users and Roles"
status: "accepted"
date: "2026-07-13"
decision_owner: "Amr Bani Irshid"
scope:
  - "MINIS Flutter mobile application"
  - "MINIS Spring Boot backend"
  - "Firebase Authentication"
  - "PostgreSQL"
tags:
  - "authentication"
  - "firebase-authentication"
  - "spring-security"
  - "identity"
  - "authorization"
supersedes: []
superseded_by: []
---

# ADR-0003: Firebase Authentication Integration with Backend-Owned Users and Roles

## Status

**Accepted**

## Context

MINIS supports:

- email and password;
- Google Sign-In;
- Apple Sign-In.

Firebase Authentication is already selected for user authentication.

MINIS still requires backend-owned application concepts that Firebase
Authentication must not control:

- internal user ID;
- local profile;
- `VIEWER`, `CREATOR`, and `ADMIN` roles;
- creator approval;
- local account status;
- suspension and disabling;
- content ownership;
- notification access;
- chat participant authorization;
- moderation permissions.

The system needs a clear identity boundary between:

```text
Who is this external user?
```

and:

```text
What may this MINIS user do?
```

The backend must verify that incoming Firebase tokens are valid without trusting
client-provided user IDs, emails, or roles.

## Decision Drivers

- official Flutter and Firebase authentication support;
- one authentication flow across email/password, Google, and Apple;
- backend-controlled authorization;
- ability to suspend a MINIS account independently of Firebase;
- stable internal identifiers;
- no Firebase service credentials in the mobile client or repository;
- idempotent account creation;
- testability without depending on production Firebase;
- low operational complexity for the MVP.

## Decision

MINIS will use Firebase Authentication as the external identity provider and
Spring Boot as the application authorization authority.

The integration follows these rules:

1. Flutter authenticates using Firebase Authentication.
2. Flutter sends the Firebase ID token as a bearer token to Spring Boot.
3. Spring Security verifies the ID token using the Firebase Admin SDK.
4. The backend reads the verified Firebase UID from the token.
5. The backend maps the Firebase UID to an internal MINIS user UUID.
6. PostgreSQL owns MINIS roles and account status.
7. Flutter calls an explicit idempotent bootstrap endpoint after sign-in.
8. Email/password accounts require verified email before protected product use.
9. Google and Apple sign-ins rely on the verified federated Firebase identity.
10. Firebase service-account credentials remain outside Git.
11. Local and integration testing use both:
    - Firebase Authentication Emulator;
    - a fake `FirebaseTokenVerifier` for focused application tests.

## Trust Boundary

```text
Firebase Authentication
    -> proves external identity and token claims

Spring Boot
    -> verifies the token
    -> maps identity to MINIS user
    -> enforces account status
    -> loads roles and permissions
    -> authorizes product actions

PostgreSQL
    -> stores MINIS user, roles, creator status, and account status
```

Firebase Authentication is not the source of truth for MINIS authorization.

## Request Authentication Flow

```text
Flutter signs in with Firebase
        |
        v
Firebase returns ID token
        |
        v
Flutter sends request:

Authorization: Bearer <Firebase ID token>
        |
        v
Spring Security authentication filter
        |
        v
FirebaseTokenVerifier verifies token
        |
        v
Extract firebase_uid and trusted claims
        |
        v
Resolve local MINIS user
        |
        +-- local user ACTIVE
        |       |
        |       v
        |   create authenticated MINIS principal
        |
        +-- local user SUSPENDED or DISABLED
        |       |
        |       v
        |   reject request
        |
        +-- local user missing
                |
                +-- bootstrap endpoint may create it
                |
                +-- other protected endpoints return bootstrap-required
                    or unauthorized application response
```

## Token Verification

The Spring Boot integration uses a small application-owned abstraction:

```java
public interface FirebaseTokenVerifier {

    VerifiedFirebaseIdentity verify(String idToken);
}
```

Production implementation:

```text
FirebaseAdminTokenVerifier
    -> Firebase Admin SDK
```

Test implementation:

```text
FakeFirebaseTokenVerifier
```

The Spring Security filter:

1. reads the bearer token;
2. rejects missing or malformed credentials on protected routes;
3. delegates verification;
4. creates an authenticated principal only from verified claims and local
   database state;
5. never accepts client-submitted role or user identifiers as authority.

The filter must not contain profile, creator, or domain business logic.

## Local MINIS User Identity

MINIS uses its own UUID primary key.

Directional model:

```text
users
-----
id                  UUID primary key
firebase_uid        unique, not null
email               nullable depending on provider data
email_verified      boolean
display_name
avatar_url
role or role mapping
status
created_at
updated_at
last_authenticated_at
```

The exact physical schema belongs in Flyway migrations and database design.

Rules:

- `firebase_uid` has a unique constraint;
- Firebase UID is not used as the primary key for all domain relationships;
- domain tables reference the internal MINIS UUID;
- provider identity can evolve without changing every domain foreign key;
- email is not treated as the stable identity key;
- changing an email does not create a new MINIS user.

## Account Bootstrap

Flutter calls:

```http
POST /api/v1/auth/bootstrap
```

after successful Firebase sign-in.

The endpoint is idempotent.

Conceptual behavior:

```text
verify Firebase token
        |
        v
find user by firebase_uid
        |
        +-- found
        |     -> synchronize allowed profile claims
        |     -> return current MINIS account
        |
        +-- missing
              -> validate account eligibility
              -> create VIEWER user
              -> return created MINIS account
```

Repeated bootstrap calls return the same local user and do not create duplicate
accounts.

New users start with:

```text
role = VIEWER
status = ACTIVE
```

Creator and Admin roles are granted only through backend-authorized operations.

Other protected product endpoints do not silently create accounts. This keeps
account creation explicit and avoids hidden writes during normal reads.

## Email Verification

For email/password authentication:

```text
email_verified must be true
```

before protected MINIS product access is granted.

The bootstrap endpoint rejects an unverified email/password identity with a
specific error that Flutter can use to show the verification flow.

For Google and Apple sign-in:

- Firebase verifies the federated sign-in;
- the system uses Firebase UID as the stable external identity;
- missing or provider-limited email data does not replace UID-based identity;
- provider-specific claims are not used to grant MINIS roles.

Guest browsing remains available without authentication according to the PRD.

## Roles and Authorization

PostgreSQL is the source of truth for:

```text
VIEWER
CREATOR
ADMIN
```

Firebase Custom Claims are not the authoritative role store for the MVP.

Reasons:

- creator approval is MINIS business data;
- suspensions must take effect from backend state;
- role updates should not wait for client token refresh;
- authorization rules can query current application state;
- the client must not infer authority from local UI state.

The authenticated principal includes:

```text
internal MINIS user ID
firebase UID
local roles
local account status
```

Controllers and application services authorize using the internal principal.

## Local Account Status

Directional statuses:

```text
ACTIVE
SUSPENDED
DISABLED
```

A valid Firebase token does not guarantee product access.

Behavior:

```text
ACTIVE
    -> normal authorized access

SUSPENDED
    -> authentication identity is known
    -> product request is rejected according to suspension policy

DISABLED
    -> product request is rejected
```

The exact response contract and whether limited read-only access is allowed
belong in API and moderation design.

## Logout and Token Lifecycle

Flutter uses Firebase logout for local authentication state.

The backend does not maintain a separate server session for the MVP.

On logout, the client also calls the device-registration disable/detach flow so
private push notifications do not remain associated with the previous user.

Expired or invalid Firebase ID tokens are rejected.

Token refresh is handled by Firebase client SDK behavior, and Flutter sends a
current token with protected requests.

## Secrets and Credentials

Firebase service-account credentials:

- are never committed to Git;
- are never shipped in the Flutter application;
- are injected into the backend through environment variables, mounted secrets,
  or an equivalent production secret mechanism;
- are scoped to the required Firebase project and permissions;
- are redacted from logs and error responses.

The VPS deployment must not expose credential files through public volumes or
web roots.

## Testing Strategy

## Unit and Slice Tests

Use `FakeFirebaseTokenVerifier` to test:

- valid identity;
- invalid token;
- expired token;
- unverified email/password user;
- missing local user;
- active user;
- suspended user;
- disabled user;
- Viewer, Creator, and Admin authorization;
- bootstrap idempotency;
- duplicate Firebase UID concurrency.

These tests do not require a network connection to Firebase.

## Integration Tests

Use Firebase Authentication Emulator to test:

- email/password sign-up;
- email verification behavior where supported by the test setup;
- Google/Apple adapter boundaries through appropriate test doubles when the
  emulator does not reproduce the full provider;
- token extraction and verification integration;
- bootstrap flow;
- logout/device detachment integration.

PostgreSQL integration tests verify unique constraints and concurrent bootstrap.

## Production Firebase

Tests must not use production users or production service credentials by
default.

## Considered Alternatives

## Backend-Owned Password Authentication — Rejected

This would require MINIS to own:

- password storage;
- reset workflows;
- email verification;
- Google and Apple OAuth integrations;
- account security operations.

It duplicates capabilities already selected through Firebase and increases
security and implementation burden.

## Trust Client-Provided Firebase UID — Rejected

The client could submit another user's UID.

Only UID extracted from a verified Firebase ID token is trusted.

## Firebase UID as Domain Primary Key — Rejected

Advantages:

- one fewer identifier;
- simple initial mapping.

Reasons for rejection:

- couples all domain tables to an external identity provider;
- makes identity-provider migration harder;
- mixes authentication identity with domain identity;
- provider changes would propagate across the schema.

## Firebase Custom Claims as the Only Role Store — Rejected

Advantages:

- roles may appear directly in verified tokens;
- fewer database reads in some designs.

Reasons for rejection:

- token claims can remain stale until refresh;
- creator approval and suspension are application data;
- backend authorization must reflect current local state;
- role management would become coupled to Firebase administration.

## Automatic User Creation on Any Protected Endpoint — Rejected

Advantages:

- fewer explicit client steps;
- convenient first request.

Reasons for rejection:

- read endpoints may unexpectedly write;
- account eligibility and verification errors become less explicit;
- provisioning behavior is spread across authentication infrastructure;
- retries and concurrent first requests are harder to reason about.

An explicit idempotent bootstrap endpoint keeps creation observable and testable.

## Separate Authentication Microservice — Rejected for MVP

The modular monolith does not need a network boundary for token verification and
local-user resolution.

A separate service would add deployment and availability dependencies without a
current independent scaling or organizational requirement.

## Consequences

### Positive

- one authentication provider across supported sign-in methods;
- backend-controlled authorization;
- local suspensions take effect from current PostgreSQL state;
- stable internal UUIDs;
- explicit and idempotent provisioning;
- credentials remain server-side;
- testable token-verifier boundary;
- no separate backend session infrastructure for MVP.

### Negative

- most protected requests require local-user resolution;
- Firebase availability and token behavior remain external dependencies;
- the client must complete bootstrap after sign-in;
- account and token errors require clear API contracts;
- logout must coordinate device-token detachment;
- local tests need both fakes and emulator setup.

## Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Client submits another user ID | Derive identity only from verified token |
| Duplicate user rows on concurrent bootstrap | Unique `firebase_uid` plus transactional upsert |
| Suspended user still has valid Firebase token | Check local account status on protected requests |
| Role changes delayed by token caching | Load authoritative PostgreSQL roles |
| Credentials leak into Git/image | External secrets and repository scanning |
| Emulator differs from production | Fake unit tests plus focused staging verification |
| Hidden user creation on reads | Explicit bootstrap endpoint only |
| Email change creates duplicate account | Map by Firebase UID, not email |
| Account switch receives old pushes | Detach/reassociate device installation |

## Implementation Constraints

- Do not accept `userId`, `firebaseUid`, or role from request bodies as
  authenticated authority.
- Do not store Firebase service-account JSON in the repository.
- Do not use email as a unique external identity replacement for Firebase UID.
- Do not grant Creator or Admin during normal bootstrap.
- Do not silently create users from arbitrary protected endpoints.
- Do not treat valid Firebase authentication as sufficient when local status is
  suspended or disabled.
- Do not put domain business rules inside the token-verification filter.

## Revisit Triggers

Reconsider this ADR when:

- MINIS introduces a backend-owned session/token layer;
- Firebase Authentication no longer meets product requirements;
- multiple identity providers must be linked outside Firebase;
- enterprise SSO is required;
- role and permission complexity requires a separate authorization model;
- token verification moves to an API gateway for a broader platform;
- a dedicated identity service becomes justified by team or system boundaries;
- account deletion or legal requirements change identity-retention behavior.

A future ADR must supersede this decision.

## References

- Firebase Authentication:
  https://firebase.google.com/docs/auth
- Verify Firebase ID tokens:
  https://firebase.google.com/docs/auth/admin/verify-id-tokens
- Firebase Authentication Emulator:
  https://firebase.google.com/docs/emulator-suite/connect_auth
