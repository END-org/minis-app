# Database Schema Management

MINIS intentionally does not use Flyway or Liquibase yet.

Local development uses Hibernate `ddl-auto=update` so the first vertical slice is quick to run. Automated tests use `ddl-auto=create-drop` against PostgreSQL Testcontainers. Production uses `ddl-auto=validate`, so schema SQL must be reviewed and applied explicitly before deployment.

Review `schema-v1.sql` before applying it to any persistent environment.
