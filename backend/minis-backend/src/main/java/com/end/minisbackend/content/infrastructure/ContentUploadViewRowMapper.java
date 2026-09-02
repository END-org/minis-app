package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentUploadView;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;

class ContentUploadViewRowMapper implements RowMapper<ContentUploadView> {
  @Override
  public ContentUploadView mapRow(ResultSet resultSet, int rowNum) throws SQLException {
    ContentCategory category = ContentCategory.valueOf(resultSet.getString("category"));
    return new ContentUploadView(
        resultSet.getObject("id", java.util.UUID.class),
        resultSet.getString("title"),
        resultSet.getString("description"),
        category.name(),
        category.displayName(),
        ContentStatus.valueOf(resultSet.getString("status")),
        resultSet.getString("upload_failure_code"),
        resultSet.getString("upload_failure_reason"),
        resultSet.getTimestamp("created_at").toInstant(),
        instantOrNull(resultSet, "upload_started_at"),
        instantOrNull(resultSet, "upload_completed_at"),
        instantOrNull(resultSet, "published_at"),
        resultSet.getString("media_content_type"),
        longOrNull(resultSet, "media_size_bytes"),
        longOrNull(resultSet, "duration_ms"));
  }

  private static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
    Timestamp timestamp = resultSet.getTimestamp(column);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static Long longOrNull(ResultSet resultSet, String column) throws SQLException {
    long value = resultSet.getLong(column);
    return resultSet.wasNull() ? null : value;
  }
}
