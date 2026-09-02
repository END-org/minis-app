package com.end.minisbackend.content.infrastructure.repository;

import com.end.minisbackend.content.application.model.feed.ContentCursor;
import com.end.minisbackend.content.application.model.feed.PublishedContentView;
import com.end.minisbackend.content.application.port.ContentQueryStore;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PublishedContentQueryPersistenceAdapter implements ContentQueryStore {
  private final JdbcTemplate jdbcTemplate;
  private final PublishedContentViewRowMapper rowMapper;

  PublishedContentQueryPersistenceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = new PublishedContentViewRowMapper();
  }

  @Override
  public List<PublishedContentView> findFeed(
      ContentCategory category, ContentCursor cursor, int limit) {
    List<Object> parameters = new ArrayList<>();
    StringBuilder sql =
        new StringBuilder(
            """
            select
            """);
    sql.append(PublishedContentSelect.COLUMNS)
        .append(
            """
            from content_items c
              join creator_profiles p on p.user_id = c.creator_user_id
            where c.status = 'PUBLISHED'
            """);
    if (category != null) {
      sql.append(" and c.category = ?");
      parameters.add(category.name());
    }
    appendCursor(sql, parameters, cursor);
    sql.append(" order by c.published_at desc, c.id desc limit ?");
    parameters.add(limit);
    return jdbcTemplate.query(sql.toString(), rowMapper, parameters.toArray());
  }

  @Override
  public List<PublishedContentView> findCreatorContent(
      UUID creatorUserId, ContentCursor cursor, int limit) {
    List<Object> parameters = new ArrayList<>();
    parameters.add(creatorUserId);
    StringBuilder sql =
        new StringBuilder(
            """
            select
            """);
    sql.append(PublishedContentSelect.COLUMNS)
        .append(
            """
            from content_items c
              join creator_profiles p on p.user_id = c.creator_user_id
            where c.status = 'PUBLISHED'
              and c.creator_user_id = ?
            """);
    appendCursor(sql, parameters, cursor);
    sql.append(" order by c.published_at desc, c.id desc limit ?");
    parameters.add(limit);
    return jdbcTemplate.query(sql.toString(), rowMapper, parameters.toArray());
  }

  @Override
  public Optional<PublishedContentView> findPublishedById(UUID contentId) {
    List<PublishedContentView> rows =
        jdbcTemplate.query(
            """
            select
            """
                + PublishedContentSelect.COLUMNS
                + """
            from content_items c
              join creator_profiles p on p.user_id = c.creator_user_id
            where c.status = 'PUBLISHED'
              and c.id = ?
            """,
            rowMapper,
            contentId);
    return rows.stream().findFirst();
  }

  private static void appendCursor(
      StringBuilder sql, List<Object> parameters, ContentCursor cursor) {
    if (cursor == null) {
      return;
    }
    sql.append(" and (c.published_at < ? or (c.published_at = ? and c.id < ?))");
    Timestamp publishedAt = Timestamp.from(cursor.publishedAt());
    parameters.add(publishedAt);
    parameters.add(publishedAt);
    parameters.add(cursor.id());
  }
}
