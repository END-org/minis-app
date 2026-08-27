package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.PublishedContentView;
import com.end.minisbackend.content.domain.ContentCategory;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

class PublishedContentViewRowMapper implements RowMapper<PublishedContentView> {
  @Override
  public PublishedContentView mapRow(ResultSet resultSet, int rowNum) throws SQLException {
    return new PublishedContentView(
        resultSet.getObject("id", java.util.UUID.class),
        resultSet.getObject("creator_user_id", java.util.UUID.class),
        resultSet.getString("public_slug"),
        resultSet.getString("public_display_name"),
        resultSet.getString("avatar_url"),
        resultSet.getBoolean("verified"),
        resultSet.getString("title"),
        resultSet.getString("description"),
        ContentCategory.valueOf(resultSet.getString("category")),
        resultSet.getString("media_object_key"),
        resultSet.getString("media_content_type"),
        resultSet.getLong("media_size_bytes"),
        durationMs(resultSet),
        resultSet.getTimestamp("published_at").toInstant());
  }

  private static Long durationMs(ResultSet resultSet) throws SQLException {
    long value = resultSet.getLong("duration_ms");
    return resultSet.wasNull() ? null : value;
  }
}
