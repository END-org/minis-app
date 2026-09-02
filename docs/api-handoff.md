# API Handoff

## Event Media Upload

`POST /api/v1/content` is the controlled live creator upload endpoint. It requires a verified Firebase bearer token that maps to an active backend `CREATOR` account and an `Idempotency-Key` request header.

Send `multipart/form-data` with `metadata` as JSON and `video` as one MP4 file:

```text
metadata: {"title":"Opening Mini","description":"A short event clip.","category":"EDUCATION"}
video: <vertical-h264-mp4>
Idempotency-Key: upload-<client-generated-uuid>
```

The accepted profile is 200 MB maximum, MP4/H.264, optional AAC, vertical 720-1080 by 1280-1920 pixels, 90 seconds maximum, and 30 fps maximum. The server generates the content and object identities. Requests never include creator IDs, object keys, signed URLs, or temporary paths.

New successfully published uploads return `201 Created` with `Location: /api/v1/content/{contentId}`. Retrying a completed logical upload with the same key and matching metadata returns `200 OK` with the original result. A duplicate while in progress returns `409 UPLOAD_IN_PROGRESS`; a duplicate after a terminal failure returns `409 UPLOAD_ALREADY_FAILED` and needs a new key. A reused key with different metadata is a conflict.

`GET /api/v1/me/uploads?cursor=&size=` returns the authenticated creator's uploads only, in bounded keyset pages. It returns lifecycle state (`UPLOADING`, `PUBLISHED`, `FAILED`, or `REMOVED`), safe failure information, and a public content projection only when published.

## Social Interactions

Authenticated active local accounts use idempotent relationship writes:

- `PUT` / `DELETE /api/v1/content/{contentId}/like`
- `PUT` / `DELETE /api/v1/content/{contentId}/save`
- `PUT` / `DELETE /api/v1/creators/{slug}/follow`

Private collections are `GET /api/v1/me/liked-content?cursor=&size=` and `GET /api/v1/me/saved-content?cursor=&size=`. Public content responses include `likeCount`, `liked`, `saved`, and `following`; guest booleans are false. The React product uses optional authenticated reads so this state is correct for the current backend session.

## Failure Contract

All errors use the shared JSON API contract and include the correlation identifier when available. Upload failures intentionally expose safe classifications and messages only. They never return Firebase token data, raw Firebase UIDs, MinIO credentials, private object keys, temporary paths, original server-side filenames, or stack traces.
