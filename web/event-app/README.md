# MINIS Event App

React event browser client for MINIS event accounts.

## Stack

- Node.js 24 LTS, declared in the root `.nvmrc` and `package.json` engines.
- npm with one committed `package-lock.json`.
- React, strict TypeScript, Vite, React Router, TanStack Query, Firebase Web SDK, Vitest, React Testing Library, and ESLint.

## Environment

Copy the frontend template for local development:

```bash
cp .env.example .env.local
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
```

Public Firebase Web SDK values:

```text
VITE_FIREBASE_API_KEY
VITE_FIREBASE_AUTH_DOMAIN
VITE_FIREBASE_PROJECT_ID
VITE_FIREBASE_APP_ID
VITE_FIREBASE_STORAGE_BUCKET
```

Local API routing:

```text
MINIS_EVENT_APP_DEV_HOST=localhost
MINIS_EVENT_APP_DEV_PORT=5173
MINIS_EVENT_APP_DEV_PROXY_TARGET=http://localhost:8080
VITE_API_BASE_URL=
```

Leave `VITE_API_BASE_URL` empty for local development and demo builds. Local development sends `/api` through the Vite proxy; demo NGINX serves React and `/api/` under one public origin. Set `VITE_API_BASE_URL` only when intentionally calling an explicitly allowed backend origin.

Firebase Admin credentials are backend-only. Do not copy Admin JSON, service-account content, prepared-account passwords, or private credentials into this app.

## Local Start

Start PostgreSQL, MinIO, and the backend first. From `web/event-app`:

```bash
npm ci
npm run dev
```

The Vite app defaults to `http://localhost:5173` with `strictPort`. If that port is busy, Vite fails instead of moving to another origin. The backend local profile allows `http://localhost:5173` by default through `MINIS_LOCAL_ALLOWED_ORIGINS`; if you intentionally change the origin, update that local backend value to the exact origin.

## Verification

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```

The build requires the public `VITE_FIREBASE_*` values. Automated tests mock Firebase SDK behavior and do not require real Firebase network access, prepared-account passwords, Firebase Console access, Admin credentials, or a live event environment.

## Authentication Flow

1. Email/password registration uses Firebase account creation, Firebase display-name update, and Firebase verification email.
2. Unverified password users stay on `/verify-email`; React does not call backend bootstrap until Firebase reports `emailVerified`.
3. Verified password users and Google users obtain a Firebase ID token in memory.
4. The typed API client attaches `Authorization: Bearer <token>` to protected backend requests.
5. React calls `POST /api/v1/auth/bootstrap`.
6. The bootstrap response seeds the UID-scoped account cache for the current Firebase user.
7. `GET /api/v1/me` remains the stable backend account projection API for explicit refreshes and future clients.
8. Role and account status are displayed only from backend responses.

Password reset uses Firebase `sendPasswordResetEmail`; Spring Boot never receives passwords. Google sign-in uses Firebase `GoogleAuthProvider` and `signInWithPopup`. Google provider availability, authorized domains, and email/reset templates must be verified in Firebase Console for the event project.

On the first retry-safe 401, the API client force-refreshes the Firebase ID token and retries once. Repeated 401 clears authenticated server-state caches, signs out, and returns to sign-in. A 403 never refreshes the token and never signs out automatically.

## Event Screens

- `/` loads the guest-accessible vertical feed from `GET /api/v1/feed`.
- `/content/:contentId` loads a public playable content projection.
- `/creators` and `/creators/:slug` load public active creators without authentication.
- `/creators/:slug` also loads public creator content from `GET /api/v1/creators/{slug}/content`.
- `/guest` keeps the simple guest/diagnostic landing surface.
- `/sign-in`, `/sign-up`, `/verify-email`, and `/forgot-password` own Firebase browser authentication flows.
- `/account` uses the provisioned backend account projection and allows the signed-in user to edit
  only display name, avatar URL, and bio.
- `/admin` and `/admin/users/:userId` are presentation-gated by the backend-owned `ADMIN` role and
  call the backend event-admin APIs for creator grant/revoke, suspension/reactivation, and
  verification.

Role, account status, and creator verification are displayed only from backend responses.

## Media Feed Behavior

The feed uses the same public projection for guests and authenticated users. Signed media URLs are
not cached as content identity and are not stored outside the TanStack Query cache. The active feed
item is detected with `IntersectionObserver`, starts muted, pauses off-screen media, and keeps only
the active item plus one neighboring item mounted as video elements. Distant items retain metadata
but release media resources.

If media fails near or after the signed URL expiration time, the client refreshes the projection once
and retries playback once. Failures well before expiration show a broken-media state and do not
create a retry loop.

## Demo Routing

The demo proxy builds this app into NGINX. `/api/`, `/healthz`, `/auth-test/`, `/swagger-ui/`, and `/v3/api-docs` are matched before SPA fallback. Unknown frontend routes return `index.html`; unknown API routes remain backend responses.

## Troubleshooting

- Missing Firebase config: set all required public `VITE_FIREBASE_*` values.
- Local NETWORK error on `/account` or `/creators`: keep `VITE_API_BASE_URL` blank, run Vite on `http://localhost:5173`, and confirm the backend is reachable at `http://localhost:8080`.
- Broken feed media with `minio:9000` in the URL: the backend signing endpoint is wrong; set
  `MINIS_CONTENT_STORAGE_PUBLIC_ENDPOINT` to the browser-reachable MinIO origin.
- Expired media URL: refresh the feed or content route. The client automatically retries once when
  the expiry timing indicates that a signed URL probably expired.
- Local API CORS failure after changing ports: run the backend with the `local` profile and set `MINIS_LOCAL_ALLOWED_ORIGINS` to the exact React origin.
- 401 after sign-in: refresh or sign in again; the client retries a safe 401 only once.
- 403 after sign-in: the Firebase identity is valid, but the backend-owned local MINIS account is disabled, suspended, unverified for password bootstrap, unprovisioned, or not authorized.
