# ADR-0004: Event Media Read Path

## Status

Accepted for the event MVP.

## Context

Phase 4 needs a convincing guest feed without opening live uploads or a media-processing platform.
The event assets are curated offline, imported by an operator, stored in MinIO, and described by
PostgreSQL metadata. Browser playback must use real object storage and must not stream full objects
through Spring Boot memory.

Representative final event assets were not committed with the repository, so final browser/device
acceptance remains a manual event rehearsal step.

## Decision

The event read path supports one bounded profile:

- Policy ID: `event-mp4-h264-aac-vertical-v1`.
- Container: MP4.
- Video codec: H.264.
- Audio codec: AAC when audio exists; audio is optional.
- Detected content type: `video/mp4`.
- Maximum size: 200 MB.
- Maximum duration: 90 seconds.
- Width: 720 to 1080 pixels.
- Height: 1280 to 1920 pixels.
- Orientation: vertical, with height greater than width.
- Frame rate: up to 30 fps.

The importer uses `ffprobe` only for inspection. It rejects unsupported files and never transcodes,
remuxes, rotates, repairs, thumbnails, or rewrites media.

Spring Boot stores content metadata and generates short-lived MinIO signed read URLs at response
time. URLs are signed against the browser-reachable MinIO origin; completed signed URLs are not
rewritten. Object-management operations use the backend internal MinIO endpoint.

The demo topology uses a dedicated browser-reachable MinIO port rather than a same-origin path
prefix, because S3 signatures are sensitive to host, path, method, and query string.

## Consequences

- Public APIs never expose raw object keys, import source keys, internal Docker hostnames, or MinIO
  credentials.
- A missing object is treated as a public media inconsistency and is logged for reconciliation.
- Existing content remains historically attributable after creator revocation, suspension, or
  profile changes until an admin explicitly marks the content `REMOVED`.
- Live upload, transcoding, thumbnail generation, alternate renditions, and CDN introduction remain
  deferred.
- Final approved event videos must still be manually verified on target desktop and mobile browsers.
