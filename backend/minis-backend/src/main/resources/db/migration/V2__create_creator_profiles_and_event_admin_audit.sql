create table creator_profiles (
  user_id uuid not null,
  public_slug varchar(80) not null,
  public_display_name varchar(120) not null,
  bio varchar(500),
  avatar_url varchar(1024),
  verified boolean not null,
  created_at timestamp(6) with time zone not null,
  updated_at timestamp(6) with time zone not null,
  constraint pk_creator_profiles primary key (user_id),
  constraint fk_creator_profiles_user_account foreign key (user_id)
    references user_accounts (id) on delete restrict,
  constraint uk_creator_profiles_public_slug unique (public_slug),
  constraint ck_creator_profiles_public_slug check
    (public_slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

create index idx_creator_profiles_display_name
  on creator_profiles (public_display_name);

create table event_admin_audit_entries (
  id uuid not null,
  actor_admin_id uuid not null,
  target_user_id uuid not null,
  action_type varchar(64) not null,
  reason varchar(300),
  correlation_id varchar(128),
  created_at timestamp(6) with time zone not null,
  constraint pk_event_admin_audit_entries primary key (id),
  constraint fk_event_admin_audit_actor foreign key (actor_admin_id)
    references user_accounts (id) on delete restrict,
  constraint fk_event_admin_audit_target foreign key (target_user_id)
    references user_accounts (id) on delete restrict,
  constraint ck_event_admin_audit_action_type check (
    action_type in (
      'CREATOR_GRANTED',
      'CREATOR_REVOKED',
      'ACCOUNT_SUSPENDED',
      'ACCOUNT_REACTIVATED',
      'CREATOR_VERIFIED',
      'CREATOR_UNVERIFIED'
    )
  )
);

create index idx_event_admin_audit_target
  on event_admin_audit_entries (target_user_id, created_at);

create index idx_event_admin_audit_actor
  on event_admin_audit_entries (actor_admin_id, created_at);
