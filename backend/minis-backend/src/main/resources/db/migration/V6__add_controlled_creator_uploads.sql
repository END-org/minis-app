alter table content_items drop constraint ck_content_items_media_content_type;
alter table content_items drop constraint ck_content_items_media_size_bytes;
alter table content_items drop constraint ck_content_items_duration_ms;
alter table content_items drop constraint ck_content_items_status;
alter table content_items drop constraint ck_content_items_published_at;
alter table content_items drop constraint ck_content_items_removed_fields;

alter table content_items alter column media_content_type drop not null;
alter table content_items alter column media_size_bytes drop not null;
alter table content_items alter column published_at drop not null;
alter table content_items alter column import_source_key drop not null;

alter table content_items add column upload_idempotency_key varchar(160);
alter table content_items add column upload_started_at timestamp(6) with time zone;
alter table content_items add column upload_completed_at timestamp(6) with time zone;
alter table content_items add column upload_failure_code varchar(64);
alter table content_items add column upload_failure_reason varchar(300);

alter table content_items add constraint uk_content_items_creator_upload_idempotency_key
  unique (creator_user_id, upload_idempotency_key);

alter table content_items add constraint ck_content_items_upload_idempotency_key check (
  upload_idempotency_key is null
  or length(btrim(upload_idempotency_key)) > 0
);

alter table content_items add constraint ck_content_items_upload_failure_code check (
  upload_failure_code is null
  or length(btrim(upload_failure_code)) > 0
);

alter table content_items add constraint ck_content_items_upload_failure_reason check (
  upload_failure_reason is null
  or length(btrim(upload_failure_reason)) > 0
);

alter table content_items add constraint ck_content_items_media_content_type check (
  media_content_type is null
  or media_content_type = 'video/mp4'
);

alter table content_items add constraint ck_content_items_media_size_bytes check (
  media_size_bytes is null
  or media_size_bytes > 0
);

alter table content_items add constraint ck_content_items_duration_ms check (
  duration_ms is null
  or duration_ms > 0
);

alter table content_items add constraint ck_content_items_status check (
  status in ('UPLOADING', 'PUBLISHED', 'FAILED', 'REMOVED')
);

alter table content_items add constraint ck_content_items_lifecycle_fields check (
  (
    status = 'UPLOADING'
    and upload_idempotency_key is not null
    and upload_started_at is not null
    and upload_completed_at is null
    and upload_failure_code is null
    and upload_failure_reason is null
    and published_at is null
    and removed_at is null
    and removal_reason is null
    and import_source_key is null
  )
  or (
    status = 'FAILED'
    and upload_idempotency_key is not null
    and upload_started_at is not null
    and upload_completed_at is not null
    and upload_failure_code is not null
    and upload_failure_reason is not null
    and published_at is null
    and removed_at is null
    and removal_reason is null
    and import_source_key is null
  )
  or (
    status = 'PUBLISHED'
    and published_at is not null
    and media_content_type = 'video/mp4'
    and media_size_bytes is not null
    and media_size_bytes > 0
    and upload_failure_code is null
    and upload_failure_reason is null
    and removed_at is null
    and removal_reason is null
    and (
      (
        import_source_key is not null
        and length(btrim(import_source_key)) > 0
        and upload_idempotency_key is null
        and upload_started_at is null
        and upload_completed_at is null
      )
      or (
        import_source_key is null
        and upload_idempotency_key is not null
        and upload_started_at is not null
        and upload_completed_at is not null
      )
    )
  )
  or (
    status = 'REMOVED'
    and published_at is not null
    and media_content_type = 'video/mp4'
    and media_size_bytes is not null
    and media_size_bytes > 0
    and removed_at is not null
    and removal_reason is not null
    and length(btrim(removal_reason)) > 0
    and upload_failure_code is null
    and upload_failure_reason is null
    and (
      (
        import_source_key is not null
        and length(btrim(import_source_key)) > 0
        and upload_idempotency_key is null
        and upload_started_at is null
        and upload_completed_at is null
      )
      or (
        import_source_key is null
        and upload_idempotency_key is not null
        and upload_started_at is not null
        and upload_completed_at is not null
      )
    )
  )
);

create index idx_content_items_creator_uploads
  on content_items (creator_user_id, created_at desc, id desc)
  where upload_idempotency_key is not null;

create index idx_content_items_stale_uploads
  on content_items (upload_started_at, id)
  where status = 'UPLOADING';
