package com.end.minisbackend.creator.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.creator.application.port.CreatorFollowStore;
import com.end.minisbackend.creator.application.service.follow.CreatorFollowService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CreatorFollowPersistenceIT {
  private static final Instant BASE_TIME = Instant.parse("2026-07-18T12:00:00Z");

  @Autowired private CreatorFollowService creatorFollowService;
  @Autowired private CreatorFollowStore creatorFollowStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void followPersistenceIsIdempotentAndUnfollowIsIdempotent() {
    UUID followerId = insertUser("follow-viewer", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("follow-creator", "CREATOR", "ACTIVE");
    MinisPrincipal follower = principal(followerId, "VIEWER", "ACTIVE");

    creatorFollowService.follow(follower, creator.slug());
    creatorFollowService.follow(follower, creator.slug());

    assertThat(followCount(followerId, creator.userId())).isEqualTo(1);
    assertThat(creatorFollowStore.followedCreatorIds(followerId, List.of(creator.userId())))
        .containsExactly(creator.userId());

    creatorFollowService.unfollow(follower, creator.slug());
    creatorFollowService.unfollow(follower, creator.slug());

    assertThat(followCount(followerId, creator.userId())).isZero();
  }

  @Test
  void concurrentDuplicateFollowCreatesOneLogicalRelationship() throws Exception {
    UUID followerId = insertUser("concurrent-follow-viewer", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("concurrent-follow-creator", "CREATOR", "ACTIVE");
    MinisPrincipal follower = principal(followerId, "VIEWER", "ACTIVE");

    List<String> results =
        runConcurrently(() -> creatorFollowService.follow(follower, creator.slug()));

    assertThat(results).containsOnly("ok");
    assertThat(followCount(followerId, creator.userId())).isEqualTo(1);
  }

  @Test
  void databaseUniqueConstraintAndSelfFollowConstraintAreActive() {
    UUID followerId = insertUser("unique-follow-viewer", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("unique-follow-creator", "CREATOR", "ACTIVE");
    insertFollow(followerId, creator.userId(), BASE_TIME);

    assertThatThrownBy(() -> insertFollow(followerId, creator.userId(), BASE_TIME.plusSeconds(1)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertFollow(creator.userId(), creator.userId(), BASE_TIME))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void selfFollowIsRejectedExplicitly() {
    CreatorFixture creator = insertCreator("self-follow-creator", "CREATOR", "ACTIVE");

    assertThatThrownBy(
            () ->
                creatorFollowService.follow(
                    principal(creator.userId(), "CREATOR", "ACTIVE"), creator.slug()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT));
    assertThat(followCount(creator.userId(), creator.userId())).isZero();
  }

  @Test
  void nonCreatorTargetCannotBeFollowed() {
    UUID followerId = insertUser("non-creator-follow-viewer", "VIEWER", "ACTIVE");
    CreatorFixture target = insertCreator("non-creator-follow-target", "VIEWER", "ACTIVE");

    assertThatThrownBy(
            () ->
                creatorFollowService.follow(
                    principal(followerId, "VIEWER", "ACTIVE"), target.slug()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));
    assertThat(followCount(followerId, target.userId())).isZero();
  }

  @Test
  void creatorRevocationPreventsNewFollowsButDoesNotDestroyHistoricalRows() {
    UUID existingFollowerId = insertUser("revoked-existing-follower", "VIEWER", "ACTIVE");
    UUID newFollowerId = insertUser("revoked-new-follower", "VIEWER", "ACTIVE");
    CreatorFixture creator = insertCreator("revoked-follow-creator", "CREATOR", "ACTIVE");
    creatorFollowService.follow(principal(existingFollowerId, "VIEWER", "ACTIVE"), creator.slug());

    jdbcTemplate.update("update user_accounts set role = 'VIEWER' where id = ?", creator.userId());

    assertThatThrownBy(
            () ->
                creatorFollowService.follow(
                    principal(newFollowerId, "VIEWER", "ACTIVE"), creator.slug()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));
    assertThat(followCount(existingFollowerId, creator.userId())).isEqualTo(1);

    creatorFollowService.unfollow(
        principal(existingFollowerId, "VIEWER", "ACTIVE"), creator.slug());

    assertThat(followCount(existingFollowerId, creator.userId())).isZero();
  }

  private List<String> runConcurrently(CheckedRunnable operation) throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    CyclicBarrier barrier = new CyclicBarrier(4);
    List<Callable<String>> tasks = new ArrayList<>();
    for (int index = 0; index < 4; index++) {
      tasks.add(
          () -> {
            barrier.await();
            operation.run();
            return "ok";
          });
    }
    try {
      List<Future<String>> futures = executorService.invokeAll(tasks);
      return futures.stream().map(CreatorFollowPersistenceIT::getUnchecked).toList();
    } finally {
      executorService.shutdownNow();
    }
  }

  private UUID insertUser(String label, String role, String status) {
    UUID userId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        insert into user_accounts (
          id, firebase_uid, email, email_verified, display_name, role, status,
          created_at, updated_at, last_authenticated_at
        ) values (?, ?, ?, true, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
        """,
        userId,
        label + "-" + userId,
        label + "@example.com",
        label,
        role,
        status);
    return userId;
  }

  private CreatorFixture insertCreator(String label, String role, String status) {
    UUID userId = insertUser(label, role, status);
    String slug = label + "-" + userId.toString().substring(0, 8);
    jdbcTemplate.update(
        """
        insert into creator_profiles (
          user_id, public_slug, public_display_name, verified, created_at, updated_at
        ) values (?, ?, ?, true, current_timestamp, current_timestamp)
        """,
        userId,
        slug,
        "Creator " + label);
    return new CreatorFixture(userId, slug);
  }

  private void insertFollow(UUID followerId, UUID creatorId, Instant createdAt) {
    jdbcTemplate.update(
        "insert into creator_follows (follower_user_id, creator_user_id, created_at) values (?, ?, ?)",
        followerId,
        creatorId,
        Timestamp.from(createdAt));
  }

  private long followCount(UUID followerId, UUID creatorId) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from creator_follows
            where follower_user_id = ?
              and creator_user_id = ?
            """,
            Long.class,
            followerId,
            creatorId);
    return count == null ? 0L : count;
  }

  private static MinisPrincipal principal(UUID userId, String role, String status) {
    return new MinisPrincipal(
        TestIdentities.googleIdentity(userId.toString()), userId, role, status);
  }

  private static String getUnchecked(Future<String> future) {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for creator follow", exception);
    } catch (java.util.concurrent.ExecutionException exception) {
      throw new AssertionError("Creator follow failed", exception);
    }
  }

  private record CreatorFixture(UUID userId, String slug) {}

  @FunctionalInterface
  private interface CheckedRunnable {
    void run() throws Exception;
  }
}
