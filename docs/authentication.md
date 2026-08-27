# Authentication And Authorization

Firebase Authentication proves external identity only. The MINIS backend verifies Firebase ID
tokens, then uses PostgreSQL-owned `UserAccount` state for role, account status, and protected
product access.

## Local Account Source Of Truth

- `/api/v1/auth/bootstrap` creates or loads the local account for a verified Firebase UID and
  returns the first backend-owned account projection for the browser session.
- New accounts are `VIEWER` and `ACTIVE` unless the local/demo-only event-admin bootstrap policy is
  explicitly enabled.
- `/api/v1/me` returns the backend-owned account ID, email projection, role, and account status.
- Firebase custom claims, client environment variables, route state, request bodies, and URL
  parameters are not role or status sources.
- Unverified Firebase password-provider identities are rejected with `EMAIL_NOT_VERIFIED` and must
  verify email before local bootstrap. Google/federated identities follow Firebase provider
  verification and do not require a second MINIS email-verification step.

## Browser Firebase Flows

The React event client owns browser Firebase sessions and never sends passwords to Spring Boot.

- `/sign-up` uses Firebase email/password account creation, updates the Firebase display name, sends
  a verification email, and waits on `/verify-email` before calling the backend.
- `/verify-email` reloads the Firebase user, force-refreshes the ID token after Firebase reports
  verification, then lets the central backend-session coordinator call
  `POST /api/v1/auth/bootstrap`.
- `/sign-in` supports email/password and Google sign-in. Unverified password users are routed to
  `/verify-email` before backend bootstrap.
- `/forgot-password` uses Firebase password reset directly; there is no Spring Boot password-reset
  endpoint.

The React app uses one backend-session coordinator for restored Firebase sessions, fresh
email/password sign-in, Google sign-in, and post-verification continuation. It never derives role or
status from Firebase claims, and it does not call `/api/v1/me` before bootstrap succeeds. The backend
remains responsible for local roles, status, creator capability, suspension, and all authorization
decisions.

## Event Admin Bootstrap

Initial event-admin bootstrap is disabled by default:

```text
MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED=false
MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS=
```

When enabled in only the `local` or `demo` profile, a comma-separated Firebase UID allowlist can
persist `ADMIN` as the initial PostgreSQL role during bootstrap. Email alone is never sufficient,
and Firebase UIDs are not returned in public responses. Enabling this policy outside local/demo
configuration fails application startup.

## Creator Role Policy

Supported backend roles are `VIEWER`, `CREATOR`, and `ADMIN`.

Only `ADMIN` can grant or revoke `CREATOR`. Granting creator capability creates or updates the
separate `CreatorProfile` needed for public creator identity. The backend persists the resulting
role in PostgreSQL; React only displays the backend response.

## Creator Revocation Policy

Revoking `CREATOR` changes the user's backend role back to `VIEWER`. It does not delete
`CreatorProfile`, audit history, or future content ownership references. Historical creator
identity remains available to admin workflows, while future creator-only actions depend on the
current backend role.

## Account Suspension Policy

Supported account statuses are `ACTIVE`, `SUSPENDED`, and the preserved existing `DISABLED`.

Suspension blocks protected product operations centrally during Firebase bearer authentication.
Admins can suspend and reactivate `ACTIVE` or `SUSPENDED` accounts through event-admin APIs.
`DISABLED` remains preserved but has no public event workflow in this phase.

## Public Creator Visibility

Public creator routes are guest-accessible:

- `GET /api/v1/creators`
- `GET /api/v1/creators/{slug}`

Only users whose backend role is currently `CREATOR` and status is `ACTIVE` are public. Suspended,
disabled, or revoked creators keep their profile row but are omitted from the public directory and
slug lookup.

Published content keeps historical creator attribution even if the creator is later revoked,
suspended, disabled, or changes profile details. Public content visibility changes only through the
content status policy: `PUBLISHED` content is visible and `REMOVED` content is hidden. New curated
imports may only target a currently active creator.

## Admin Audit

Privileged event-admin operations append audit rows for creator grant/revoke, suspension/reactivate,
and creator verify/unverify. Audit rows store the actor admin ID, target user ID, action type,
bounded reason, timestamp, and correlation ID. They do not store tokens, Firebase UID values,
request bodies, secrets, or profile snapshots.
