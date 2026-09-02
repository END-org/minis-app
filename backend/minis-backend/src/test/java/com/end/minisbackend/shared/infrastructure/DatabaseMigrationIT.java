package com.end.minisbackend.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.MinisBackendApplication;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

class DatabaseMigrationIT {
  private static final String POSTGRES_IMAGE = "postgres:17-alpine";
  private static final String DATABASE_NAME = "minis_migration";
  private static final String DATABASE_USER = "minis";
  private static final String DATABASE_PASSWORD = "minis";

  @Test
  void cleanDatabaseMigratesHibernateValidatesAndRepeatStartupDoesNotMutateSchema()
      throws SQLException {
    try (PostgreSQLContainer<?> database = postgresContainer()) {
      database.start();

      try (ConfigurableApplicationContext ignored = startApplication(database, "demo", "test")) {
        assertThat(
                queryForInt(database, "select count(*) from flyway_schema_history where success"))
            .isEqualTo(6);
        assertThat(tableExists(database, "user_accounts")).isTrue();
        assertThat(tableExists(database, "creator_profiles")).isTrue();
        assertThat(tableExists(database, "event_admin_audit_entries")).isTrue();
        assertThat(tableExists(database, "content_items")).isTrue();
        assertThat(tableExists(database, "content_likes")).isTrue();
        assertThat(tableExists(database, "content_saves")).isTrue();
        assertThat(tableExists(database, "creator_follows")).isTrue();
        assertThat(indexExists(database, "idx_content_items_unfiltered_public_feed")).isTrue();
        assertThat(indexExists(database, "idx_content_items_published_reconciliation")).isTrue();
        assertThat(indexExists(database, "idx_content_likes_content_id")).isTrue();
        assertThat(indexExists(database, "idx_content_likes_user_created_at_content_id")).isTrue();
        assertThat(indexExists(database, "idx_content_saves_user_created_at_content_id")).isTrue();
        assertThat(indexExists(database, "idx_content_items_creator_uploads")).isTrue();
        assertThat(indexExists(database, "idx_content_items_stale_uploads")).isTrue();
        assertThat(constraintExists(database, "pk_content_likes")).isTrue();
        assertThat(constraintExists(database, "pk_content_saves")).isTrue();
        assertThat(constraintExists(database, "pk_creator_follows")).isTrue();
        assertThat(constraintExists(database, "ck_creator_follows_no_self_follow")).isTrue();
        assertThat(constraintExists(database, "uk_content_items_creator_upload_idempotency_key"))
            .isTrue();
        assertThat(constraintExists(database, "ck_content_items_lifecycle_fields")).isTrue();
        assertThat(columnIsNullable(database, "content_items", "media_content_type")).isTrue();
        assertThat(columnIsNullable(database, "content_items", "media_size_bytes")).isTrue();
        assertThat(columnIsNullable(database, "content_items", "published_at")).isTrue();
        assertThat(columnIsNullable(database, "content_items", "import_source_key")).isTrue();
      }

      List<String> schemaAfterFirstStartup = schemaFingerprint(database);

      try (ConfigurableApplicationContext ignored = startApplication(database, "demo", "test")) {}

      assertThat(schemaFingerprint(database)).containsExactlyElementsOf(schemaAfterFirstStartup);
    }
  }

  @Test
  void migratedSchemaEnforcesIdentityConstraints() throws SQLException {
    try (PostgreSQLContainer<?> database = postgresContainer()) {
      database.start();

      try (ConfigurableApplicationContext ignored = startApplication(database, "test")) {}

      insertUser(database, "constraint-user", "VIEWER", "ACTIVE");
      UUID creatorUserId = userId(database, "constraint-user");
      insertUser(database, "constraint-follower", "VIEWER", "ACTIVE");
      UUID followerUserId = userId(database, "constraint-follower");
      insertUser(database, "bad-slug-user", "VIEWER", "ACTIVE");
      UUID badSlugUserId = userId(database, "bad-slug-user");

      assertThatThrownBy(() -> insertUser(database, "constraint-user", "VIEWER", "ACTIVE"))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertUser(database, "invalid-role-user", "OWNER", "ACTIVE"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(() -> insertUser(database, "invalid-status-user", "VIEWER", "LOCKED"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      insertCreatorProfile(database, creatorUserId, "event-creator");
      UUID contentId =
          insertContent(
              database, creatorUserId, "source-1", "PUBLISHED", "EDUCATION", 1024L, 30_000L);
      assertThatThrownBy(() -> insertCreatorProfile(database, creatorUserId, "event-creator"))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertCreatorProfile(database, badSlugUserId, "bad slug"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      insertAudit(database, creatorUserId, creatorUserId, "CREATOR_GRANTED");
      assertThatThrownBy(() -> insertAudit(database, creatorUserId, creatorUserId, "ROLE_CHANGED"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      insertLike(database, followerUserId, contentId);
      insertSave(database, followerUserId, contentId);
      insertFollow(database, followerUserId, creatorUserId);
      insertReservedUpload(database, creatorUserId, "creator-upload-key");
      insertFailedUpload(database, creatorUserId, "creator-failed-upload-key");
      assertThatThrownBy(() -> insertLike(database, followerUserId, contentId))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertSave(database, followerUserId, contentId))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertFollow(database, followerUserId, creatorUserId))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertFollow(database, creatorUserId, creatorUserId))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(
              () ->
                  insertContent(
                      database, creatorUserId, "source-1", "PUBLISHED", "EDUCATION", 1L, 1L))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(
              () ->
                  insertContent(
                      database, creatorUserId, "source-2", "PUBLISHED", "UNKNOWN", 1L, 1L))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(
              () ->
                  insertContent(database, creatorUserId, "source-3", "HIDDEN", "EDUCATION", 1L, 1L))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(
              () ->
                  insertContent(
                      database, creatorUserId, "source-4", "PUBLISHED", "EDUCATION", 0L, 1L))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(
              () ->
                  insertContent(
                      database, creatorUserId, "source-5", "PUBLISHED", "EDUCATION", 1L, 0L))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(() -> insertReservedUpload(database, creatorUserId, "creator-upload-key"))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertInvalidUploadingWithPublishedAt(database, creatorUserId))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(
              () -> insertInvalidPublishedUploadWithoutCompletion(database, creatorUserId))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
    }
  }

  @Test
  void demoProfileFailsWhenRequiredDatabaseUrlIsMissing() {
    assertThatThrownBy(
            () ->
                new SpringApplicationBuilder(MinisBackendApplication.class)
                    .profiles("demo", "test")
                    .web(WebApplicationType.SERVLET)
                    .run(
                        "--server.port=0",
                        "--spring.datasource.url=${MINIS_TEST_MISSING_DATASOURCE_URL}",
                        "--spring.datasource.driver-class-name=org.postgresql.Driver",
                        "--spring.datasource.username=" + DATABASE_USER,
                        "--spring.datasource.password=" + DATABASE_PASSWORD,
                        "--minis.firebase.project-id=demo-test"))
        .satisfies(
            throwable ->
                assertThat(causalMessages(throwable))
                    .anySatisfy(
                        message ->
                            assertThat(message).contains("MINIS_TEST_MISSING_DATASOURCE_URL")));
  }

  private static PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(POSTGRES_IMAGE)
        .withDatabaseName(DATABASE_NAME)
        .withUsername(DATABASE_USER)
        .withPassword(DATABASE_PASSWORD);
  }

  private static ConfigurableApplicationContext startApplication(
      PostgreSQLContainer<?> database, String... profiles) {
    return new SpringApplicationBuilder(MinisBackendApplication.class)
        .profiles(profiles)
        .web(WebApplicationType.SERVLET)
        .run(
            "--server.port=0",
            "--spring.datasource.url=" + database.getJdbcUrl(),
            "--spring.datasource.driver-class-name=org.postgresql.Driver",
            "--spring.datasource.username=" + database.getUsername(),
            "--spring.datasource.password=" + database.getPassword(),
            "--spring.jpa.hibernate.ddl-auto=validate",
            "--spring.flyway.enabled=true",
            "--spring.flyway.baseline-on-migrate=false",
            "--spring.flyway.clean-disabled=true",
            "--minis.firebase.project-id=demo-test",
            "--minis.http.allowed-origins=");
  }

  private static boolean tableExists(PostgreSQLContainer<?> database, String tableName)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = ?
                """)) {
      statement.setString(1, tableName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1) == 1;
      }
    }
  }

  private static boolean indexExists(PostgreSQLContainer<?> database, String indexName)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                "select count(*) from pg_indexes where schemaname = 'public' and indexname = ?")) {
      statement.setString(1, indexName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1) == 1;
      }
    }
  }

  private static boolean constraintExists(PostgreSQLContainer<?> database, String constraintName)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement("select count(*) from pg_constraint where conname = ?")) {
      statement.setString(1, constraintName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1) == 1;
      }
    }
  }

  private static boolean columnIsNullable(
      PostgreSQLContainer<?> database, String tableName, String columnName) throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """)) {
      statement.setString(1, tableName);
      statement.setString(2, columnName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return "YES".equals(resultSet.getString(1));
      }
    }
  }

  private static List<String> schemaFingerprint(PostgreSQLContainer<?> database)
      throws SQLException {
    List<String> fingerprint = new ArrayList<>();
    fingerprint.addAll(
        queryForStrings(
            database,
            """
            select table_name
              || '.' || column_name
              || ':' || data_type
              || ':' || coalesce(character_maximum_length::text, '')
              || ':' || coalesce(datetime_precision::text, '')
              || ':' || is_nullable
            from information_schema.columns
            where table_schema = 'public'
            order by table_name, ordinal_position
            """));
    fingerprint.addAll(
        queryForStrings(
            database,
            """
            select conname
              || ':' || contype::text
              || ':' || pg_get_constraintdef(pg_constraint.oid)
            from pg_constraint
            where connamespace = 'public'::regnamespace
            order by conname
            """));
    fingerprint.addAll(
        queryForStrings(
            database,
            """
            select indexname || ':' || indexdef
            from pg_indexes
            where schemaname = 'public'
            order by indexname
            """));
    return fingerprint;
  }

  private static void insertUser(
      PostgreSQLContainer<?> database, String firebaseUid, String role, String status)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into user_accounts (
                  id,
                  firebase_uid,
                  email_verified,
                  role,
                  status,
                  created_at,
                  updated_at,
                  last_authenticated_at
                ) values (?, ?, true, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setString(2, firebaseUid);
      statement.setString(3, role);
      statement.setString(4, status);
      statement.executeUpdate();
    }
  }

  private static UUID userId(PostgreSQLContainer<?> database, String firebaseUid)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement("select id from user_accounts where firebase_uid = ?")) {
      statement.setString(1, firebaseUid);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getObject(1, UUID.class);
      }
    }
  }

  private static void insertCreatorProfile(
      PostgreSQLContainer<?> database, UUID userId, String slug) throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into creator_profiles (
                  user_id,
                  public_slug,
                  public_display_name,
                  verified,
                  created_at,
                  updated_at
                ) values (?, ?, 'Event Creator', false, current_timestamp, current_timestamp)
                """)) {
      statement.setObject(1, userId);
      statement.setString(2, slug);
      statement.executeUpdate();
    }
  }

  private static void insertAudit(
      PostgreSQLContainer<?> database, UUID actorId, UUID targetId, String action)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into event_admin_audit_entries (
                  id,
                  actor_admin_id,
                  target_user_id,
                  action_type,
                  created_at
                ) values (?, ?, ?, ?, current_timestamp)
                """)) {
      statement.setObject(1, UUID.randomUUID());
      statement.setObject(2, actorId);
      statement.setObject(3, targetId);
      statement.setString(4, action);
      statement.executeUpdate();
    }
  }

  private static UUID insertContent(
      PostgreSQLContainer<?> database,
      UUID creatorUserId,
      String importSourceKey,
      String status,
      String category,
      long mediaSizeBytes,
      Long durationMs)
      throws SQLException {
    UUID contentId = UUID.randomUUID();
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_items (
                  id,
                  creator_user_id,
                  title,
                  description,
                  category,
                  media_object_key,
                  media_content_type,
                  media_size_bytes,
                  duration_ms,
                  status,
                  import_source_key,
                  created_at,
                  published_at,
                  removed_at,
                  removal_reason
                ) values (
                  ?, ?, 'Event title', 'Event description', ?, ?, 'video/mp4', ?, ?, ?, ?,
                  current_timestamp, current_timestamp,
                  case when ? = 'REMOVED' then current_timestamp else null end,
                  case when ? = 'REMOVED' then 'admin removal' else null end
                )
                """)) {
      statement.setObject(1, contentId);
      statement.setObject(2, creatorUserId);
      statement.setString(3, category);
      statement.setString(4, "content/imports/" + importSourceKey + ".mp4");
      statement.setLong(5, mediaSizeBytes);
      if (durationMs == null) {
        statement.setNull(6, java.sql.Types.BIGINT);
      } else {
        statement.setLong(6, durationMs);
      }
      statement.setString(7, status);
      statement.setString(8, importSourceKey);
      statement.setString(9, status);
      statement.setString(10, status);
      statement.executeUpdate();
    }
    return contentId;
  }

  private static void insertLike(PostgreSQLContainer<?> database, UUID userId, UUID contentId)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_likes (
                  user_id, content_id, created_at
                ) values (?, ?, current_timestamp)
                """)) {
      statement.setObject(1, userId);
      statement.setObject(2, contentId);
      statement.executeUpdate();
    }
  }

  private static void insertSave(PostgreSQLContainer<?> database, UUID userId, UUID contentId)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_saves (
                  user_id, content_id, created_at
                ) values (?, ?, current_timestamp)
                """)) {
      statement.setObject(1, userId);
      statement.setObject(2, contentId);
      statement.executeUpdate();
    }
  }

  private static void insertFollow(
      PostgreSQLContainer<?> database, UUID followerUserId, UUID creatorUserId)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into creator_follows (
                  follower_user_id, creator_user_id, created_at
                ) values (?, ?, current_timestamp)
                """)) {
      statement.setObject(1, followerUserId);
      statement.setObject(2, creatorUserId);
      statement.executeUpdate();
    }
  }

  private static void insertReservedUpload(
      PostgreSQLContainer<?> database, UUID creatorUserId, String idempotencyKey)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_items (
                  id,
                  creator_user_id,
                  title,
                  description,
                  category,
                  media_object_key,
                  status,
                  created_at,
                  upload_idempotency_key,
                  upload_started_at
                ) values (
                  ?, ?, 'Queued upload', 'Queued description', 'EDUCATION', ?, 'UPLOADING',
                  current_timestamp, ?, current_timestamp
                )
                """)) {
      UUID contentId = UUID.randomUUID();
      statement.setObject(1, contentId);
      statement.setObject(2, creatorUserId);
      statement.setString(3, "content/uploads/" + contentId + ".mp4");
      statement.setString(4, idempotencyKey);
      statement.executeUpdate();
    }
  }

  private static void insertFailedUpload(
      PostgreSQLContainer<?> database, UUID creatorUserId, String idempotencyKey)
      throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_items (
                  id,
                  creator_user_id,
                  title,
                  description,
                  category,
                  media_object_key,
                  status,
                  created_at,
                  upload_idempotency_key,
                  upload_started_at,
                  upload_completed_at,
                  upload_failure_code,
                  upload_failure_reason
                ) values (
                  ?, ?, 'Failed upload', 'Failed description', 'EDUCATION', ?, 'FAILED',
                  current_timestamp, ?, current_timestamp, current_timestamp,
                  'UNSUPPORTED_MEDIA_PROFILE', 'Uploaded video is not an accepted event MP4.'
                )
                """)) {
      UUID contentId = UUID.randomUUID();
      statement.setObject(1, contentId);
      statement.setObject(2, creatorUserId);
      statement.setString(3, "content/uploads/" + contentId + ".mp4");
      statement.setString(4, idempotencyKey);
      statement.executeUpdate();
    }
  }

  private static void insertInvalidUploadingWithPublishedAt(
      PostgreSQLContainer<?> database, UUID creatorUserId) throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_items (
                  id,
                  creator_user_id,
                  title,
                  description,
                  category,
                  media_object_key,
                  status,
                  created_at,
                  published_at,
                  upload_idempotency_key,
                  upload_started_at
                ) values (
                  ?, ?, 'Invalid upload', 'Invalid description', 'EDUCATION', ?, 'UPLOADING',
                  current_timestamp, current_timestamp, 'invalid-upload-key', current_timestamp
                )
                """)) {
      UUID contentId = UUID.randomUUID();
      statement.setObject(1, contentId);
      statement.setObject(2, creatorUserId);
      statement.setString(3, "content/uploads/" + contentId + ".mp4");
      statement.executeUpdate();
    }
  }

  private static void insertInvalidPublishedUploadWithoutCompletion(
      PostgreSQLContainer<?> database, UUID creatorUserId) throws SQLException {
    try (Connection connection = connection(database);
        PreparedStatement statement =
            connection.prepareStatement(
                """
                insert into content_items (
                  id,
                  creator_user_id,
                  title,
                  description,
                  category,
                  media_object_key,
                  media_content_type,
                  media_size_bytes,
                  status,
                  created_at,
                  published_at,
                  upload_idempotency_key,
                  upload_started_at
                ) values (
                  ?, ?, 'Invalid published upload', 'Invalid description', 'EDUCATION', ?,
                  'video/mp4', 1024, 'PUBLISHED', current_timestamp, current_timestamp,
                  'invalid-published-upload-key', current_timestamp
                )
                """)) {
      UUID contentId = UUID.randomUUID();
      statement.setObject(1, contentId);
      statement.setObject(2, creatorUserId);
      statement.setString(3, "content/uploads/" + contentId + ".mp4");
      statement.executeUpdate();
    }
  }

  private static List<String> causalMessages(Throwable throwable) {
    List<String> messages = new ArrayList<>();
    Throwable current = throwable;
    while (current != null) {
      if (current.getMessage() != null) {
        messages.add(current.getMessage());
      }
      current = current.getCause();
    }
    return messages;
  }

  private static int queryForInt(PostgreSQLContainer<?> database, String sql) throws SQLException {
    try (Connection connection = connection(database);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  private static List<String> queryForStrings(PostgreSQLContainer<?> database, String sql)
      throws SQLException {
    List<String> values = new ArrayList<>();
    try (Connection connection = connection(database);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        values.add(resultSet.getString(1));
      }
    }
    return values;
  }

  private static Connection connection(PostgreSQLContainer<?> database) throws SQLException {
    return DriverManager.getConnection(
        database.getJdbcUrl(), database.getUsername(), database.getPassword());
  }

  private static void assertSqlState(Throwable throwable, String sqlState) {
    assertThat(throwable).isInstanceOf(SQLException.class);
    assertThat(((SQLException) throwable).getSQLState()).isEqualTo(sqlState);
  }
}
