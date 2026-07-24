create table user_accounts (
  id uuid not null,
  firebase_uid varchar(128) not null,
  email varchar(320),
  email_verified boolean not null,
  display_name varchar(120),
  avatar_url varchar(1024),
  bio varchar(500),
  role varchar(32) not null,
  status varchar(32) not null,
  created_at timestamp(6) with time zone not null,
  updated_at timestamp(6) with time zone not null,
  last_authenticated_at timestamp(6) with time zone not null,
  constraint pk_user_accounts primary key (id),
  constraint uk_user_account_firebase_uid unique (firebase_uid),
  constraint ck_user_accounts_role check (role in ('VIEWER', 'CREATOR', 'ADMIN')),
  constraint ck_user_accounts_status check (status in ('ACTIVE', 'SUSPENDED', 'DISABLED'))
);

create index idx_user_accounts_email on user_accounts (email);
