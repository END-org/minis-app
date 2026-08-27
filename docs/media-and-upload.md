# Media And Upload

## Phase 4 Scope

Phase 4 implements a read-only event media path:

curated local/demo asset -> `ffprobe` inspection -> MinIO object -> PostgreSQL metadata -> public
feed and creator content APIs.

Live creator upload, presigned upload, transcoding, thumbnails, reports, analytics, social actions,
and media-processing workers are intentionally deferred.

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

## Inspection-Only Import

Install `ffprobe` from FFmpeg and verify it is on PATH:

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
      "sourceKey": "event-2026-creator-a-intro-v1",
      "creatorSlug": "creator-a",
      "title": "Opening Mini",
      "description": "A short fictional event clip for importer rehearsal.",
      "category": "EDUCATION"
    }
  ]
}
```

`creatorUserId` may be used instead of `creatorSlug` when the operator has a backend UUID. Absolute
paths, parent traversal, duplicate source keys, invalid categories, inactive creators, and revoked
creators are rejected before upload.

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
default 10 minute lifetime, and are never persisted.

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

Rerunning the same manifest is idempotent when the metadata and object match. If upload succeeds but
database persistence fails, the importer attempts to delete only the newly uploaded object and
reports the failed entry.

## Reconciliation

Run reconciliation as a one-off command mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--minis.content.reconciliation.enabled=true --minis.content.reconciliation.run-id=<safe-run-id>"
```

It checks published metadata for missing objects and logs `public_media_inconsistency_detected`.
It does not invent rows, delete content, or mark a broken item as successful.

## Public APIs

- `GET /api/v1/categories`
- `GET /api/v1/feed?cursor=&limit=&category=`
- `GET /api/v1/content/{contentId}`
- `GET /api/v1/creators/{slug}/content?cursor=&limit=`
- `PUT /api/v1/admin/content/{contentId}/removal`

The public projection includes content ID, title, description, category, creator attribution, media
content type, size, optional duration, signed URL, and signed URL expiration. It does not expose raw
object keys, import source keys, MinIO credentials, Firebase UIDs, or internal hostnames.

Cursor order is `publishedAt DESC, id DESC`. The cursor encodes that pair and is opaque to clients.
Malformed, oversized, or unsupported cursors return the standard validation error.

## Visibility And Removal

Only `PUBLISHED` content is public. `REMOVED` content is absent from feed, detail, creator content,
and category results.

Only `ADMIN` may remove content. Removal is idempotent, preserves the row and object history, records
`removedAt` plus a bounded reason, and emits a safe `content_removed` operational log. The admin
audit table is not extended in this phase.

Creator revocation, suspension, disabled status, and profile edits do not delete content. Existing
published content remains historically attributable until explicit content removal.

## Range, Proxy, And CORS

The selected design uses a dedicated browser-reachable MinIO origin or port. Range requests go
directly to MinIO and must preserve the signed host, path, method, query string, and `Range` header.
The backend test suite verifies MinIO returns `206 Partial Content` for a signed Range request.

The demo NGINX proxy does not path-prefix signed media URLs. MinIO CORS is not configured by default
because plain video element playback does not require JavaScript-readable cross-origin responses.
Add bucket CORS only after testing a target browser path that genuinely needs it.

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
