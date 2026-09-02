package com.end.minisbackend.content.infrastructure.repository;

import com.end.minisbackend.content.application.model.upload.ContentUploadCursor;
import com.end.minisbackend.content.application.model.upload.ContentUploadView;
import com.end.minisbackend.content.application.port.ContentUploadStore;
import com.end.minisbackend.content.domain.model.ContentItem;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ContentUploadPersistenceAdapter implements ContentUploadStore {
  private static final String UPLOAD_COLUMNS =
      """
      id,
      title,
      description,
      category,
      status,
      upload_failure_code,
      upload_failure_reason,
      created_at,
      upload_started_at,
      upload_completed_at,
      published_at,
      media_content_type,
      media_size_bytes,
      duration_ms
      """;

  private final ContentItemRepository repository;
  private final JdbcTemplate jdbcTemplate;
  private final ContentUploadViewRowMapper rowMapper;

  ContentUploadPersistenceAdapter(ContentItemRepository repository, JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = new ContentUploadViewRowMapper();
  }

  @Override
  public Optional<ContentItem> findById(UUID contentId) {
    return repository.findById(contentId);
  }

  @Override
  public Optional<ContentItem> findByCreatorUserIdAndUploadIdempotencyKey(
      UUID creatorUserId, String uploadIdempotencyKey) {
    return repository.findByCreatorUserIdAndUploadIdempotencyKey(
        creatorUserId, uploadIdempotencyKey);
  }

  @Override
  public Optional<ContentUploadView> findUploadByCreatorUserIdAndId(
      UUID creatorUserId, UUID contentId) {
    List<ContentUploadView> rows =
        jdbcTemplate.query(
            """
            select
            """
                + UPLOAD_COLUMNS
                + """
            from content_items
            where creator_user_id = ?
              and id = ?
              and upload_idempotency_key is not null
            """,
            rowMapper,
            creatorUserId,
            contentId);
    return rows.stream().findFirst();
  }

  @Override
  public List<ContentUploadView> findUploadsByCreatorUserId(
      UUID creatorUserId, ContentUploadCursor cursor, int limit) {
    ArrayList<Object> parameters = new ArrayList<>();
    parameters.add(creatorUserId);
    StringBuilder sql =
        new StringBuilder(
            """
            select
            """);
    sql.append(UPLOAD_COLUMNS)
        .append(
            """
            from content_items
            where creator_user_id = ?
              and upload_idempotency_key is not null
            """);
    appendCursor(sql, parameters, cursor);
    sql.append(" order by created_at desc, id desc limit ?");
    parameters.add(limit);
    return jdbcTemplate.query(sql.toString(), rowMapper, parameters.toArray());
  }

  @Override
  public ContentItem saveAndFlush(ContentItem contentItem) {
    return repository.saveAndFlush(contentItem);
  }

  @Override
  public boolean publishedContentOwnsObject(UUID contentId, String objectKey) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from content_items
            where id = ?
              and media_object_key = ?
              and status = 'PUBLISHED'
            """,
            Integer.class,
            contentId,
            objectKey);
    return count != null && count > 0;
  }

  private static void appendCursor(
      StringBuilder sql, ArrayList<Object> parameters, ContentUploadCursor cursor) {
    if (cursor == null) {
      return;
    }
    sql.append(" and (created_at < ? or (created_at = ? and id < ?))");
    Timestamp createdAt = Timestamp.from(cursor.createdAt());
    parameters.add(createdAt);
    parameters.add(createdAt);
    parameters.add(cursor.contentId());
  }
}
