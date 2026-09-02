# Media And Upload

## Phase 5 Scope

The normal event media path is controlled creator upload:

Creator React app -> authenticated multipart request -> server-owned temporary spool -> `ffprobe` inspection -> private MinIO object -> PostgreSQL publication -> public feed.

The Phase 4 curated importer remains an explicit recovery/demo path. It is not the normal product workflow. Direct-to-cloud uploads, resumable uploads, provider-specific multipart orchestration, transcoding, thumbnails, subtitles, reports, analytics, and media-processing workers remain deferred.

## Supported Event Profile

Policy ID: `event-mp4-h264-aac-vertical-v1`.

- Container: MP4.
- Video codec: H.264.
- Audio codec: AAC when audio exists. Audio is optional.
- Detected content type: `video/mp4`.
- Maximum file size: 200 MB.
- Maximum duration: 90 seconds.
- Width: 720 to 1080 pixels.
- Height: 1280 to 1920 pixels.
- Orientation: vertical.
- Frame rate: 30 fps or lower.

The profile is implemented and tested with synthetic probe metadata. Final event assets and target
devices still require manual playback verification because the repository does not contain private
event media.

Target matrix for manual rehearsal:

- Desktop: current Chrome, Edge, Firefox, and Safari where available.
- Mobile: the actual event iPhone and Android browser targets.

## Controlled Creator Upload

`POST /api/v1/content` accepts `multipart/form-data` with these parts:

- `metadata`: JSON with `title`, `description`, and `category`.
- `video`: one MP4 video file.
- `Idempotency-Key`: an 8-160 character request identity header.

Only an authenticated, active, backend-provisioned `CREATOR` may upload. Creator ownership comes from the verified backend principal, not a request field. The server generates the content ID and private `content/uploads/<content-id>.mp4` object key; neither the raw key nor a temporary path is returned.

The backend rejects empty files, files over 200 MB, unsafe filenames, a non-`.mp4` filename, a MIME type other than `video/mp4`, a missing MP4 `ftyp` signature, malformed/timeout probe output, and media outside the accepted profile. Spring accepts a 200 MB file with a 205 MB multipart request ceiling. The demo proxy uses the same 205 MB ceiling and 120-second request/proxy transfer timeouts; storage has a bounded two-minute write timeout.

The lifecycle is `UPLOADING` -> `PUBLISHED` or `FAILED`; published items may later become `REMOVED`. `UPLOADING` and `FAILED` are never public. My Uploads returns the owner-only state, safe failure code/reason, timestamps, and a public content projection only after publication.

Idempotency is creator-scoped. A new key reserves and executes one logical upload. A retry after a lost successful response returns the same `PUBLISHED` result. A duplicate during `UPLOADING` returns `UPLOAD_IN_PROGRESS`; a duplicate after `FAILED` returns `UPLOAD_ALREADY_FAILED` and requires a new key; metadata that differs for a reused key returns a conflict. The uniqueness constraint makes concurrent duplicates converge on one logical upload.

The operation uses short database transactions: reserve the `UPLOADING` row, stream/spool and validate outside a transaction, upload to MinIO, then publish in a short transaction. If storage or publication fails after object upload, the row becomes `FAILED` and the service deletes the new object unless a committed published row owns it. Cleanup failure is recorded as an orphan candidate for operations; it is not exposed to clients.

## Inspection-Only Import

Install `ffprobe` from FFmpeg and verify it is on PATH for host-based importer runs. The backend
container installs the same dependency in its runtime image:

```bash
ffprobe -version
```

The probe uses `ProcessBuilder` without shell string concatenation, has a hard timeout, parses JSON
output, bounds captured output, and rejects missing or malformed metadata. It never modifies the
source file.

## Manifest Format

The real event manifest must remain untracked. Use relative asset paths rooted under the configured
assets directory:

```json
{
  "entries": [
    {
      "assetPath": "creator-a/intro.mp4",
      "importSourceKey": "event-2026-creator-a-intro-v1",
      "creatorSlug": "creator-a",
      "title": "Opening Mini",
      "description": "A short fictional event clip for importer rehearsal.",
      "category": "EDUCATION"
    }
  ]
}
```

`creatorUserId` may be used instead of `creatorSlug` when the operator has a backend UUID. Absolute
paths, parent traversal, symlink escapes, duplicate source keys, invalid categories, inactive
creators, and revoked creators are rejected before upload.

## Storage And Signed URLs

Local defaults:

- Internal MinIO endpoint: `http://localhost:9000`.
- Browser-reachable signing endpoint: `http://localhost:9000`.
- Bucket: `minis-event-media`.

Demo defaults:

- Internal MinIO endpoint: `http://minio:9000`.
- Browser-reachable signing endpoint: `http://localhost:9002`.
- Bucket: `minis-event-media`.

The signing endpoint must be exactly the scheme, host, port, and path the browser uses. Do not
rewrite a completed signed URL. Signed URLs are generated only while building API responses, have a
default 10 minute lifetime, and are never persisted. Object existence is checked before a signed URL
is returned, so missing media produces an explicit backend failure and safe error log instead of a
plausible URL for an absent object.

Storage connect, read, and write timeouts default to 5 seconds, 30 seconds, and 2 minutes. Storage is
included in the readiness health group; health responses do not expose endpoint or credential
details.

The React client treats signed URLs as replaceable media references, not content identity. If a
media error occurs near or after expiry, it refreshes the content projection once and retries once.
Failures well before expiry show a broken-media state without assuming expiry.

## Importer Command

Run the importer only as an explicit command mode after PostgreSQL and MinIO are available. Example
from `backend/minis-backend`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.importer.enabled=true --minis.content.importer.manifest-path=<path-to-private-manifest.json> --minis.content.importer.assets-root=<path-to-private-assets-root> --minis.content.importer.run-id=<safe-run-id>"
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--minis.content.importer.enabled=true --minis.content.importer.manifest-path=<path-to-private-manifest.json> --minis.content.importer.assets-root=<path-to-private-assets-root> --minis.content.importer.run-id=<safe-run-id>"
```

Set `MINIS_CONTENT_STORAGE_AUTO_CREATE_BUCKET=true` only for disposable local/demo runs when bucket
creation is intended. Production-like environments should provision the bucket separately.

Rerunning the same manifest is idempotent when normalized metadata and the object match. If upload
succeeds but database persistence fails, the importer attempts to delete only the newly uploaded
object and reports the failed entry. Compensation first checks whether a concurrent committed row
owns the deterministic object key; cleanup failure is logged as an orphan candidate.

## Reconciliation

Run reconciliation as a one-off command mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.reconciliation.enabled=true --minis.content.reconciliation.run-id=<safe-run-id>"
```

It first marks bounded batches of `UPLOADING` rows older than `MINIS_CONTENT_UPLOAD_STALE_AGE` (30 minutes by default) as `FAILED` with a safe stale-upload reason. It then checks published metadata in bounded batches for missing objects and logs `public_media_inconsistency_detected`. It does not invent rows, delete content, or mark a broken item as successful.

## Public APIs

- `POST /api/v1/content` with multipart `metadata`, `video`, and `Idempotency-Key` for active creators.
- `GET /api/v1/me/uploads?cursor=&size=` for the owner-only upload history.
- `GET /api/v1/categories`
- `GET /api/v1/feed?cursor=&limit=&category=`
- `GET /api/v1/content/{contentId}`
- `GET /api/v1/creators/{slug}/content?cursor=&limit=`
- `PUT /api/v1/content/{contentId}/like`
- `DELETE /api/v1/content/{contentId}/like`
- `PUT /api/v1/content/{contentId}/save`
- `DELETE /api/v1/content/{contentId}/save`
- `PUT /api/v1/creators/{slug}/follow`
- `DELETE /api/v1/creators/{slug}/follow`
- `GET /api/v1/me/liked-content?cursor=&size=`
- `GET /api/v1/me/saved-content?cursor=&size=`
- `PUT /api/v1/admin/content/{contentId}/removal`

The public projection includes content ID, title, description, category, creator attribution, media
content type, size, optional duration, signed URL, signed URL expiration, derived `likeCount`, and
viewer-state booleans `liked`, `saved`, and `following`. It does not expose raw object keys, import
source keys, MinIO credentials, Firebase UIDs, internal hostnames, or public save counts.

Cursor order is `publishedAt DESC, id DESC`. The cursor encodes that pair and is opaque to clients.
Malformed, oversized, or unsupported cursors return the standard validation error.

## Social Relationships

Likes and saves are PostgreSQL relationships between the authenticated local account and
`PUBLISHED` content. Follows are PostgreSQL relationships between an authenticated local account and
a currently public active creator identified by canonical creator slug. `PUT` social operations
ensure a relationship exists and `DELETE` social operations ensure it does not exist, so repeated
and concurrent duplicate requests are idempotent.

Guest feed/content/creator-content reads keep the same response shape as authenticated reads. For
guests, `liked`, `saved`, and `following` are `false`; `likeCount` remains visible. If a request
supplies an invalid bearer token, the existing authentication filter rejects it instead of treating
the request as a guest read.

Liked and saved collections are private `/me` routes. The client never supplies another user's ID.
Collection order is `relationshipCreatedAt DESC, contentId DESC`, with an opaque cursor containing
the validated ordering pair. Page size is bounded with the same content API default and maximum:
default `10`, maximum `30`, minimum clamped to `1`.

`content_likes` has the primary key `(user_id, content_id)` and an index on `content_id` to support
derived like counts. `content_likes` and `content_saves` each have `(user_id, created_at DESC,
content_id DESC)` indexes for private collection pagination. `creator_follows` has the primary key
`(follower_user_id, creator_user_id)`, which supports follow existence and page-level viewer-follow
lookup without adding a redundant index.

Creator revocation, suspension, or disabled status prevents new follows because the target is no
longer public active. Existing follow rows are not automatically destroyed; unfollow remains
idempotently possible by historical slug where the creator profile still exists. Content published
before revocation remains historically attributable until explicit content removal.

## Visibility And Removal

Only `PUBLISHED` content is public. `REMOVED` content is absent from feed, detail, creator content,
and category results.

Only `ADMIN` may remove content. Removal is idempotent, preserves the row and object history, records
`removedAt` plus a bounded reason, and emits a safe `content_removed` operational log. The admin
audit table is not extended in this phase.

Creator revocation, suspension, disabled status, and profile edits do not delete content. Existing
published content remains historically attributable until explicit content removal.

Existing like/save rows may remain when content is later marked `REMOVED`. Removed content is
excluded from feed, detail, creator content, and private liked/saved collection projections. New
likes and saves against removed content fail safely, while unlike and unsave remain successful
idempotent no-op operations where the viewer has no relationship.

## Range, Proxy, And CORS

The selected design uses a dedicated browser-reachable MinIO origin or port. Range requests go
directly to MinIO and must preserve the signed host, path, method, query string, and `Range` header.
The backend test suite verifies MinIO returns `206 Partial Content` for a signed Range request.

The demo NGINX proxy does not path-prefix signed media URLs. MinIO CORS is not configured by default
because plain video element playback does not require JavaScript-readable cross-origin responses.
Add bucket CORS only after testing a target browser path that genuinely needs it.

The API CORS policy allows `Authorization`, `Content-Type`, `X-Correlation-ID`, and `Idempotency-Key` from the configured local/demo origin. It does not allow credentialed wildcard origins.

## Golden Snapshot

After importing approved event assets:

1. Record the Flyway schema version.
2. Record manifest identifier, media-policy ID, PUBLISHED count, REMOVED count, object count, and
   asset checksums.
3. Back up PostgreSQL first, then mirror/export the MinIO bucket.
4. Store backups outside Git with access controls appropriate for event media.

Example commands:

```bash
docker compose -f compose.demo.yml exec postgres pg_dump -U minis_demo minis_event_demo > minis-event-demo.sql
mc mirror --overwrite minis-demo/minis-event-media ./minis-event-media-backup
```

Restore order:

1. Restore PostgreSQL.
2. Restore the MinIO bucket.
3. Run reconciliation.
4. Verify row counts, object counts, Range response, feed API, creator content API, and browser
   playback.

## Troubleshooting

- Broken playback with `minio:9000` in the browser URL: the signing endpoint is wrong; set
  `MINIS_CONTENT_STORAGE_PUBLIC_ENDPOINT` to the browser-reachable origin.
- Signature mismatch: do not rewrite the host, path, query string, or signed URL after generation.
- Expired URLs: refresh the feed or content projection; do not cache signed URLs as stable IDs.
- Missing object: run reconciliation and compare imported source keys against the private manifest.
- Unsupported media: inspect with `ffprobe` and provide a file that matches the event policy.
