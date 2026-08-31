create table content_likes (
  user_id uuid not null,
  content_id uuid not null,
  created_at timestamp(6) with time zone not null,
  constraint pk_content_likes primary key (user_id, content_id),
  constraint fk_content_likes_user foreign key (user_id)
    references user_accounts (id) on delete restrict,
  constraint fk_content_likes_content foreign key (content_id)
    references content_items (id) on delete restrict
);

create index idx_content_likes_content_id
  on content_likes (content_id);

create index idx_content_likes_user_created_at_content_id
  on content_likes (user_id, created_at desc, content_id desc);

create table content_saves (
  user_id uuid not null,
  content_id uuid not null,
  created_at timestamp(6) with time zone not null,
  constraint pk_content_saves primary key (user_id, content_id),
  constraint fk_content_saves_user foreign key (user_id)
    references user_accounts (id) on delete restrict,
  constraint fk_content_saves_content foreign key (content_id)
    references content_items (id) on delete restrict
);

create index idx_content_saves_user_created_at_content_id
  on content_saves (user_id, created_at desc, content_id desc);

create table creator_follows (
  follower_user_id uuid not null,
  creator_user_id uuid not null,
  created_at timestamp(6) with time zone not null,
  constraint pk_creator_follows primary key (follower_user_id, creator_user_id),
  constraint fk_creator_follows_follower foreign key (follower_user_id)
    references user_accounts (id) on delete restrict,
  constraint fk_creator_follows_creator foreign key (creator_user_id)
    references user_accounts (id) on delete restrict,
  constraint ck_creator_follows_no_self_follow check (follower_user_id <> creator_user_id)
);
