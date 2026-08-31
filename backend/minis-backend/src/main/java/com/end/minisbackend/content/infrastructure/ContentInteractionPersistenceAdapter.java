package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentInteractionCursor;
import com.end.minisbackend.content.application.ContentInteractionStore;
import com.end.minisbackend.content.application.PersonalContentView;
import com.end.minisbackend.content.application.PublishedContentView;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ContentInteractionPersistenceAdapter implements ContentInteractionStore {
  private static final String LIKE_TABLE = "content_likes";
  private static final String SAVE_TABLE = "content_saves";

  private final JdbcTemplate jdbcTemplate;
  private final PublishedContentViewRowMapper contentRowMapper;

  ContentInteractionPersistenceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.contentRowMapper = new PublishedContentViewRowMapper();
  }

  @Override
  public int ensureLikedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
    return ensureContentRelationship(LIKE_TABLE, "pk_content_likes", userId, contentId, createdAt);
  }

  @Override
  public boolean isLiked(UUID userId, UUID contentId) {
    return contentRelationshipExists(LIKE_TABLE, userId, contentId);
  }

  @Override
  public void removeLike(UUID userId, UUID contentId) {
    removeContentRelationship(LIKE_TABLE, userId, contentId);
  }

  @Override
  public int ensureSavedIfPublished(UUID userId, UUID contentId, Instant createdAt) {
    return ensureContentRelationship(SAVE_TABLE, "pk_content_saves", userId, contentId, createdAt);
  }

  @Override
  public boolean isSaved(UUID userId, UUID contentId) {
    return contentRelationshipExists(SAVE_TABLE, userId, contentId);
  }

  @Override
  public void removeSave(UUID userId, UUID contentId) {
    removeContentRelationship(SAVE_TABLE, userId, contentId);
  }

  @Override
  public boolean publishedContentExists(UUID contentId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from content_items
            where id = ?
              and status = 'PUBLISHED'
            """,
            Integer.class,
            contentId);
    return count != null && count > 0;
  }

  @Override
  public Map<UUID, Long> likeCountsByContentIds(List<UUID> contentIds) {
    if (contentIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, Long> counts = new HashMap<>();
    jdbcTemplate.query(
        "select content_id, count(*) from content_likes where content_id in ("
            + placeholders(contentIds.size())
            + ") group by content_id",
        resultSet -> {
          counts.put(resultSet.getObject("content_id", UUID.class), resultSet.getLong(2));
        },
        contentIds.toArray());
    return Map.copyOf(counts);
  }

  @Override
  public Set<UUID> likedContentIds(UUID userId, List<UUID> contentIds) {
    return contentRelationshipContentIds(LIKE_TABLE, userId, contentIds);
  }

  @Override
  public Set<UUID> savedContentIds(UUID userId, List<UUID> contentIds) {
    return contentRelationshipContentIds(SAVE_TABLE, userId, contentIds);
  }

  @Override
  public List<PersonalContentView> findLikedContent(
      UUID userId, ContentInteractionCursor cursor, int limit) {
    return findPersonalContent(LIKE_TABLE, userId, cursor, limit);
  }

  @Override
  public List<PersonalContentView> findSavedContent(
      UUID userId, ContentInteractionCursor cursor, int limit) {
    return findPersonalContent(SAVE_TABLE, userId, cursor, limit);
  }

  private int ensureContentRelationship(
      String tableName, String constraintName, UUID userId, UUID contentId, Instant createdAt) {
    return jdbcTemplate.update(
        """
        insert into
        """
            + tableName
            + """
             (user_id, content_id, created_at)
        select ?, c.id, ?
        from content_items c
        where c.id = ?
          and c.status = 'PUBLISHED'
        on conflict on constraint
        """
            + constraintName
            + """
         do nothing
        """,
        userId,
        Timestamp.from(createdAt),
        contentId);
  }

  private boolean contentRelationshipExists(String tableName, UUID userId, UUID contentId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from " + tableName + " where user_id = ? and content_id = ?",
            Integer.class,
            userId,
            contentId);
    return count != null && count > 0;
  }

  private void removeContentRelationship(String tableName, UUID userId, UUID contentId) {
    jdbcTemplate.update(
        "delete from " + tableName + " where user_id = ? and content_id = ?", userId, contentId);
  }

  private Set<UUID> contentRelationshipContentIds(
      String tableName, UUID userId, List<UUID> contentIds) {
    if (contentIds.isEmpty()) {
      return Set.of();
    }
    Set<UUID> matches = new HashSet<>();
    Object[] parameters = parameters(userId, contentIds);
    jdbcTemplate.query(
        "select content_id from "
            + tableName
            + " where user_id = ? and content_id in ("
            + placeholders(contentIds.size())
            + ")",
        resultSet -> {
          matches.add(resultSet.getObject("content_id", UUID.class));
        },
        parameters);
    return Set.copyOf(matches);
  }

  private List<PersonalContentView> findPersonalContent(
      String tableName, UUID userId, ContentInteractionCursor cursor, int limit) {
    java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
    parameters.add(userId);
    StringBuilder sql =
        new StringBuilder(
            """
            select
            """);
    sql.append(PublishedContentSelect.COLUMNS)
        .append(
            """
            ,
            r.created_at as relationship_created_at
            from
            """)
        .append(tableName)
        .append(
            """
             r
              join content_items c on c.id = r.content_id
              join creator_profiles p on p.user_id = c.creator_user_id
            where r.user_id = ?
              and c.status = 'PUBLISHED'
            """);
    appendCursor(sql, parameters, cursor);
    sql.append(" order by r.created_at desc, r.content_id desc limit ?");
    parameters.add(limit);
    return jdbcTemplate.query(
        sql.toString(),
        (resultSet, rowNumber) -> {
          PublishedContentView content = contentRowMapper.mapRow(resultSet, rowNumber);
          return new PersonalContentView(
              content, resultSet.getTimestamp("relationship_created_at").toInstant());
        },
        parameters.toArray());
  }

  private static void appendCursor(
      StringBuilder sql, List<Object> parameters, ContentInteractionCursor cursor) {
    if (cursor == null) {
      return;
    }
    sql.append(" and (r.created_at < ? or (r.created_at = ? and r.content_id < ?))");
    Timestamp relationshipCreatedAt = Timestamp.from(cursor.relationshipCreatedAt());
    parameters.add(relationshipCreatedAt);
    parameters.add(relationshipCreatedAt);
    parameters.add(cursor.contentId());
  }

  private static Object[] parameters(UUID userId, List<UUID> contentIds) {
    java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
    parameters.add(userId);
    parameters.addAll(contentIds);
    return parameters.toArray();
  }

  private static String placeholders(int count) {
    return String.join(", ", Collections.nCopies(count, "?"));
  }
}
