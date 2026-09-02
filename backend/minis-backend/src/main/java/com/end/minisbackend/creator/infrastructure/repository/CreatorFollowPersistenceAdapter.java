package com.end.minisbackend.creator.infrastructure.repository;

import com.end.minisbackend.creator.application.port.CreatorFollowStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CreatorFollowPersistenceAdapter implements CreatorFollowStore {
  private final JdbcTemplate jdbcTemplate;

  CreatorFollowPersistenceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public int ensureFollowIfCreatorIsPublic(
      UUID followerUserId, UUID creatorUserId, Instant createdAt) {
    return jdbcTemplate.update(
        """
        insert into creator_follows (follower_user_id, creator_user_id, created_at)
        select ?, p.user_id, ?
        from creator_profiles p
          join user_accounts u on u.id = p.user_id
        where p.user_id = ?
          and u.role = 'CREATOR'
          and u.status = 'ACTIVE'
        on conflict on constraint pk_creator_follows do nothing
        """,
        followerUserId,
        Timestamp.from(createdAt),
        creatorUserId);
  }

  @Override
  public boolean isFollowing(UUID followerUserId, UUID creatorUserId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from creator_follows
            where follower_user_id = ?
              and creator_user_id = ?
            """,
            Integer.class,
            followerUserId,
            creatorUserId);
    return count != null && count > 0;
  }

  @Override
  public void removeFollow(UUID followerUserId, UUID creatorUserId) {
    jdbcTemplate.update(
        """
        delete from creator_follows
        where follower_user_id = ?
          and creator_user_id = ?
        """,
        followerUserId,
        creatorUserId);
  }

  @Override
  public Optional<UUID> findCreatorUserIdBySlug(String slug) {
    List<UUID> creatorIds =
        jdbcTemplate.query(
            """
            select user_id
            from creator_profiles
            where public_slug = ?
            """,
            (resultSet, rowNumber) -> resultSet.getObject("user_id", UUID.class),
            slug);
    return creatorIds.stream().findFirst();
  }

  @Override
  public Set<UUID> followedCreatorIds(UUID followerUserId, List<UUID> creatorUserIds) {
    if (creatorUserIds.isEmpty()) {
      return Set.of();
    }
    java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
    parameters.add(followerUserId);
    parameters.addAll(creatorUserIds);
    Set<UUID> matches = new HashSet<>();
    jdbcTemplate.query(
        "select creator_user_id from creator_follows where follower_user_id = ? "
            + "and creator_user_id in ("
            + placeholders(creatorUserIds.size())
            + ")",
        resultSet -> {
          matches.add(resultSet.getObject("creator_user_id", UUID.class));
        },
        parameters.toArray());
    return Set.copyOf(matches);
  }

  private static String placeholders(int count) {
    return String.join(", ", Collections.nCopies(count, "?"));
  }
}
