package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentCommandStore;
import com.end.minisbackend.content.application.ContentCursor;
import com.end.minisbackend.content.application.ContentQueryStore;
import com.end.minisbackend.content.application.PublishedContentView;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.content.domain.ContentStatus;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ContentPersistenceAdapter implements ContentCommandStore, ContentQueryStore {
  private static final String PUBLIC_CONTENT_COLUMNS =
      """
      c.id,
      c.creator_user_id,
      p.public_slug,
      p.public_display_name,
      p.avatar_url,
      p.verified,
      c.title,
      c.description,
      c.category,
      c.media_object_key,
      c.media_content_type,
      c.media_size_bytes,
      c.duration_ms,
      c.published_at
      """;

  private final ContentItemRepository repository;
  private final JdbcTemplate jdbcTemplate;
  private final PublishedContentViewRowMapper rowMapper;

  ContentPersistenceAdapter(ContentItemRepository repository, JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = new PublishedContentViewRowMapper();
  }

  @Override
  public Optional<ContentItem> findByImportSourceKey(String importSourceKey) {
    return repository.findByImportSourceKey(importSourceKey);
  }

  @Override
  public Optional<ContentItem> findById(UUID contentId) {
    return repository.findById(contentId);
  }

  @Override
  public List<ContentItem> findPublishedItems() {
    return repository.findAll().stream()
        .filter(content -> content.status() == ContentStatus.PUBLISHED)
        .toList();
  }

  @Override
  public ContentItem saveAndFlush(ContentItem contentItem) {
    return repository.saveAndFlush(contentItem);
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
    sql.append(PUBLIC_CONTENT_COLUMNS)
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
    sql.append(PUBLIC_CONTENT_COLUMNS)
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
                + PUBLIC_CONTENT_COLUMNS
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
