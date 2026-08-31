package com.end.minisbackend.content.infrastructure;

final class PublishedContentSelect {
  static final String COLUMNS =
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

  private PublishedContentSelect() {}
}
