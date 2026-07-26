package com.end.minisbackend.admin.infrastructure;

import com.end.minisbackend.admin.application.AdminUserPage;
import com.end.minisbackend.admin.application.AdminUserQueryStore;
import com.end.minisbackend.admin.application.AdminUserView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class AdminUserQueryPersistenceAdapter implements AdminUserQueryStore {
  private static final String ADMIN_USER_COLUMNS =
      """
      u.id,
      u.email,
      u.display_name,
      u.avatar_url,
      u.bio,
      u.role,
      u.status,
      u.created_at,
      u.updated_at,
      p.user_id as creator_user_id,
      p.public_slug,
      p.public_display_name,
      p.bio as creator_bio,
      p.avatar_url as creator_avatar_url,
      p.verified,
      p.created_at as creator_created_at,
      p.updated_at as creator_updated_at
      """;

  private final JdbcTemplate jdbcTemplate;
  private final AdminUserViewRowMapper rowMapper;

  AdminUserQueryPersistenceAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = new AdminUserViewRowMapper();
  }

  @Override
  public AdminUserPage findUsers(String query, int page, int size) {
    String search = searchPattern(query);
    List<AdminUserView> users =
        jdbcTemplate.query(
            """
            select
            """
                + ADMIN_USER_COLUMNS
                + """
            from user_accounts u
              left join creator_profiles p on p.user_id = u.id
            where cast(? as text) is null
              or lower(coalesce(u.email, '')) like ?
              or lower(coalesce(u.display_name, '')) like ?
              or lower(coalesce(p.public_slug, '')) like ?
              or lower(coalesce(p.public_display_name, '')) like ?
            order by u.created_at desc, u.id
            limit ? offset ?
            """,
            rowMapper,
            search,
            search,
            search,
            search,
            search,
            size + 1,
            page * size);
    boolean hasNext = users.size() > size;
    List<AdminUserView> pageItems = hasNext ? users.subList(0, size) : users;
    return new AdminUserPage(pageItems, page, size, hasNext);
  }

  @Override
  public Optional<AdminUserView> findUser(UUID userId) {
    List<AdminUserView> users =
        jdbcTemplate.query(
            """
            select
            """
                + ADMIN_USER_COLUMNS
                + """
            from user_accounts u
              left join creator_profiles p on p.user_id = u.id
            where u.id = ?
            """,
            rowMapper,
            userId);
    return users.stream().findFirst();
  }

  private static String searchPattern(String query) {
    if (!StringUtils.hasText(query)) {
      return null;
    }
    return "%" + query.trim().toLowerCase() + "%";
  }
}
