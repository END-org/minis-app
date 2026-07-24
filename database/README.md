# Database Schema Management

Flyway owns the executable MINIS schema under `backend/minis-backend/src/main/resources/db/migration`.

The initial identity migration is:

- `V1__create_identity_user_accounts.sql`

Hibernate validates the migrated schema in local, test, demo, and production-like profiles. Application startup does not use `ddl-auto=update`, `create`, or `create-drop`.

`schema-v1.sql` is retained as a human-readable reference for the Phase 1 identity baseline. Do not apply it separately to Flyway-managed databases.

Disposable local volumes can be recreated explicitly:

```bash
docker compose -f compose.local.yml down -v
docker compose -f compose.local.yml up -d
```

Do not point Flyway at an old Hibernate-created schema unless a backup and baseline rehearsal have been completed outside normal application startup.
