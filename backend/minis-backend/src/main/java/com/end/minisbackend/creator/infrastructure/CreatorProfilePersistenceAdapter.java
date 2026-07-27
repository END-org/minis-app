package com.end.minisbackend.creator.infrastructure;

import com.end.minisbackend.creator.application.CreatorDirectoryPage;
import com.end.minisbackend.creator.application.CreatorProfileStore;
import com.end.minisbackend.creator.application.CreatorProfileView;
import com.end.minisbackend.creator.domain.CreatorProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CreatorProfilePersistenceAdapter implements CreatorProfileStore {
  private static final String PUBLIC_CREATOR_COLUMNS =
      """
      p.user_id,
      p.public_slug,
      p.public_display_name,
      p.bio,
      p.avatar_url,
      p.verified,
      p.created_at,
      p.updated_at
      """;

  private final CreatorProfileRepository repository;
  private final JdbcTemplate jdbcTemplate;
  private final CreatorProfileViewRowMapper rowMapper;

  CreatorProfilePersistenceAdapter(CreatorProfileRepository repository, JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = new CreatorProfileViewRowMapper();
  }

  @Override
  public Optional<CreatorProfile> findByUserId(UUID userId) {
    return repository.findByUserId(userId);
  }

  @Override
  public Optional<CreatorProfileView> findPublicBySlug(String slug) {
    List<CreatorProfileView> creators =
        jdbcTemplate.query(
            """
            select
            """
                + PUBLIC_CREATOR_COLUMNS
                + """
            from creator_profiles p
              join user_accounts u on u.id = p.user_id
            where p.public_slug = ?
              and u.role = 'CREATOR'
              and u.status = 'ACTIVE'
            """,
            rowMapper,
            slug);
    return creators.stream().findFirst();
  }

  @Override
  public CreatorDirectoryPage findPublicCreators(int page, int size) {
    List<CreatorProfileView> creators =
        jdbcTemplate.query(
            """
            select
            """
                + PUBLIC_CREATOR_COLUMNS
                + """
            from creator_profiles p
              join user_accounts u on u.id = p.user_id
            where u.role = 'CREATOR'
              and u.status = 'ACTIVE'
            order by lower(p.public_display_name), p.user_id
            limit ? offset ?
            """,
            rowMapper,
            size + 1,
            page * size);
    boolean hasNext = creators.size() > size;
    List<CreatorProfileView> pageItems = hasNext ? creators.subList(0, size) : creators;
    return new CreatorDirectoryPage(pageItems, page, size, hasNext);
  }

  @Override
  public List<CreatorProfileView> findByUserIds(List<UUID> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return repository.findByUserIdIn(userIds).stream()
        .map(
            profile ->
                new CreatorProfileView(
                    profile.userId(),
                    profile.publicSlug(),
                    profile.publicDisplayName(),
                    profile.bio(),
                    profile.avatarUrl(),
                    profile.verified(),
                    profile.createdAt(),
                    profile.updatedAt()))
        .toList();
  }

  @Override
  public CreatorProfile saveAndFlush(CreatorProfile profile) {
    return repository.saveAndFlush(profile);
  }
}
