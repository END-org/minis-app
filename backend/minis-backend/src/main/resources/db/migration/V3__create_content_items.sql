create table content_items (
  id uuid not null,
  creator_user_id uuid not null,
  title varchar(140) not null,
  description varchar(1000) not null,
  category varchar(64) not null,
  media_object_key varchar(512) not null,
  media_content_type varchar(100) not null,
  media_size_bytes bigint not null,
  duration_ms bigint,
  status varchar(32) not null,
  import_source_key varchar(160) not null,
  created_at timestamp(6) with time zone not null,
  published_at timestamp(6) with time zone not null,
  removed_at timestamp(6) with time zone,
  removal_reason varchar(300),
  constraint pk_content_items primary key (id),
  constraint fk_content_items_creator foreign key (creator_user_id)
    references user_accounts (id) on delete restrict,
  constraint uk_content_items_import_source_key unique (import_source_key),
  constraint ck_content_items_title check (length(btrim(title)) > 0),
  constraint ck_content_items_description check (length(btrim(description)) > 0),
  constraint ck_content_items_category check (
    category in (
      'EDUCATION',
      'TECHNOLOGY',
      'BUSINESS',
      'HEALTH',
      'SCIENCE',
      'HISTORY',
      'SKILLS',
      'MINI_DOCUMENTARY',
      'STORYTELLING',
      'CAREER_ADVICE',
      'LIFE_TIPS',
      'CINEMATIC_STORIES',
      'CREATIVE_FILMMAKING'
    )
  ),
  constraint ck_content_items_media_object_key check (length(btrim(media_object_key)) > 0),
  constraint ck_content_items_media_content_type check (media_content_type = 'video/mp4'),
  constraint ck_content_items_media_size_bytes check (media_size_bytes > 0),
  constraint ck_content_items_duration_ms check (duration_ms is null or duration_ms > 0),
  constraint ck_content_items_status check (status in ('PUBLISHED', 'REMOVED')),
  constraint ck_content_items_published_at check (published_at is not null),
  constraint ck_content_items_removed_fields check (
    (
      status = 'PUBLISHED'
      and removed_at is null
      and removal_reason is null
    )
    or (
      status = 'REMOVED'
      and removed_at is not null
      and removal_reason is not null
      and length(btrim(removal_reason)) > 0
    )
  )
);

create index idx_content_items_public_feed
  on content_items (category, published_at desc, id desc)
  where status = 'PUBLISHED';

create index idx_content_items_creator_feed
  on content_items (creator_user_id, published_at desc, id desc)
  where status = 'PUBLISHED';

create index idx_content_items_status_category
  on content_items (status, category);
