# MINIS Event App

React event browser client for prepared MINIS accounts.

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
VITE_API_BASE_URL=http://localhost:8080
```

Leave `VITE_API_BASE_URL` empty in demo builds. The demo NGINX proxy serves React and `/api/` under one public origin.

Firebase Admin credentials are backend-only. Do not copy Admin JSON, service-account content, prepared-account passwords, or private credentials into this app.

## Local Start

Start PostgreSQL, MinIO, and the backend first. From `web/event-app`:

```bash
npm ci
npm run dev
```

The Vite app defaults to `http://localhost:5173`. The backend local profile allows that origin by default.

## Verification

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```

The build requires the public `VITE_FIREBASE_*` values. Automated tests mock Firebase SDK behavior and do not require real Firebase network access, prepared-account passwords, Firebase Console access, Admin credentials, or a live event environment.

## Authentication Flow

1. A prepared user signs in with Firebase email/password.
2. Firebase Web SDK provides an ID token in memory.
3. The typed API client attaches `Authorization: Bearer <token>` to protected backend requests.
4. React calls `POST /api/v1/auth/bootstrap`.
5. React loads `GET /api/v1/me`.
6. Role and account status are displayed only from backend responses.

On the first retry-safe 401, the API client force-refreshes the Firebase ID token and retries once. Repeated 401 clears authenticated server-state caches, signs out, and returns to sign-in. A 403 never refreshes the token and never signs out automatically.

## Demo Routing

The demo proxy builds this app into NGINX. `/api/`, `/healthz`, `/auth-test/`, `/swagger-ui/`, and `/v3/api-docs` are matched before SPA fallback. Unknown frontend routes return `index.html`; unknown API routes remain backend responses.

## Troubleshooting

- Missing Firebase config: set all required public `VITE_FIREBASE_*` values.
- Local API CORS failure: run the backend with the `local` profile or set `MINIS_ALLOWED_ORIGINS=http://localhost:5173`.
- 401 after sign-in: refresh or sign in again; the client retries a safe 401 only once.
- 403 after sign-in: the Firebase identity is valid, but the backend-owned local MINIS account is disabled, suspended, unprovisioned, or not authorized.
