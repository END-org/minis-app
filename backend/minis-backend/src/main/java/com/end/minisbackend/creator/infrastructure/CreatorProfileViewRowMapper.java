package com.end.minisbackend.creator.infrastructure;

import com.end.minisbackend.creator.application.CreatorProfileView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

class CreatorProfileViewRowMapper implements RowMapper<CreatorProfileView> {
  @Override
  public CreatorProfileView mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return new CreatorProfileView(
        resultSet.getObject("user_id", UUID.class),
        resultSet.getString("public_slug"),
        resultSet.getString("public_display_name"),
        resultSet.getString("bio"),
        resultSet.getString("avatar_url"),
        resultSet.getBoolean("verified"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant());
  }
}
