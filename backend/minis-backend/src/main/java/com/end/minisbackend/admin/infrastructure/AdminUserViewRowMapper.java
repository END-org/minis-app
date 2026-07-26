package com.end.minisbackend.admin.infrastructure;

import com.end.minisbackend.admin.application.AdminUserView;
import com.end.minisbackend.creator.application.CreatorProfileView;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

class AdminUserViewRowMapper implements RowMapper<AdminUserView> {
  @Override
  public AdminUserView mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return new AdminUserView(
        resultSet.getObject("id", UUID.class),
        resultSet.getString("email"),
        resultSet.getString("display_name"),
        resultSet.getString("avatar_url"),
        resultSet.getString("bio"),
        resultSet.getString("role"),
        resultSet.getString("status"),
        resultSet.getTimestamp("created_at").toInstant(),
        resultSet.getTimestamp("updated_at").toInstant(),
        creatorProfile(resultSet));
  }

  private static CreatorProfileView creatorProfile(ResultSet resultSet) throws SQLException {
    UUID creatorUserId = resultSet.getObject("creator_user_id", UUID.class);
    if (creatorUserId == null) {
      return null;
    }
    return new CreatorProfileView(
        creatorUserId,
        resultSet.getString("public_slug"),
        resultSet.getString("public_display_name"),
        resultSet.getString("creator_bio"),
        resultSet.getString("creator_avatar_url"),
        resultSet.getBoolean("verified"),
        resultSet.getTimestamp("creator_created_at").toInstant(),
        resultSet.getTimestamp("creator_updated_at").toInstant());
  }
}
