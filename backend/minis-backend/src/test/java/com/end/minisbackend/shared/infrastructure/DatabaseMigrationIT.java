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
            .isEqualTo(2);
        assertThat(tableExists(database, "user_accounts")).isTrue();
        assertThat(tableExists(database, "creator_profiles")).isTrue();
        assertThat(tableExists(database, "event_admin_audit_entries")).isTrue();
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
      insertUser(database, "bad-slug-user", "VIEWER", "ACTIVE");
      UUID badSlugUserId = userId(database, "bad-slug-user");

      assertThatThrownBy(() -> insertUser(database, "constraint-user", "VIEWER", "ACTIVE"))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertUser(database, "invalid-role-user", "OWNER", "ACTIVE"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      assertThatThrownBy(() -> insertUser(database, "invalid-status-user", "VIEWER", "LOCKED"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      insertCreatorProfile(database, creatorUserId, "event-creator");
      assertThatThrownBy(() -> insertCreatorProfile(database, creatorUserId, "event-creator"))
          .satisfies(throwable -> assertSqlState(throwable, "23505"));
      assertThatThrownBy(() -> insertCreatorProfile(database, badSlugUserId, "bad slug"))
          .satisfies(throwable -> assertSqlState(throwable, "23514"));
      insertAudit(database, creatorUserId, creatorUserId, "CREATOR_GRANTED");
      assertThatThrownBy(() -> insertAudit(database, creatorUserId, creatorUserId, "ROLE_CHANGED"))
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
