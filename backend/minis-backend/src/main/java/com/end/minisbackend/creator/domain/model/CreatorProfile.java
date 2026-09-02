package com.end.minisbackend.creator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Entity
@Table(
    name = "creator_profiles",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_creator_profiles_public_slug", columnNames = "public_slug"),
    indexes =
        @Index(name = "idx_creator_profiles_display_name", columnList = "public_display_name"))
public class CreatorProfile {
  private static final int DISPLAY_NAME_MAX = 120;
  private static final int BIO_MAX = 500;
  private static final int AVATAR_URL_MAX = 1024;

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "public_slug", nullable = false, unique = true, length = CreatorSlug.MAX_LENGTH)
  private String publicSlug;

  @Column(name = "public_display_name", nullable = false, length = DISPLAY_NAME_MAX)
  private String publicDisplayName;

  @Column(length = BIO_MAX)
  private String bio;

  @Column(name = "avatar_url", length = AVATAR_URL_MAX)
  private String avatarUrl;

  @Column(nullable = false)
  private boolean verified;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected CreatorProfile() {}

  private CreatorProfile(
      UUID userId,
      String publicSlug,
      String publicDisplayName,
      String bio,
      String avatarUrl,
      Instant now) {
    this.userId = userId;
    this.publicSlug = publicSlug;
    this.publicDisplayName = publicDisplayName;
    this.bio = bio;
    this.avatarUrl = avatarUrl;
    this.verified = false;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static CreatorProfile create(
      UUID userId, String slug, String displayName, String bio, String avatarUrl, Instant now) {
    return new CreatorProfile(
        Objects.requireNonNull(userId, "userId is required"),
        CreatorSlug.normalize(slug),
        requiredBounded(displayName, DISPLAY_NAME_MAX),
        bounded(bio, BIO_MAX),
        boundedHttpUrl(avatarUrl, AVATAR_URL_MAX),
        Objects.requireNonNull(now, "now is required"));
  }

  public void updateGrantProfile(
      String slug, String displayName, String bio, String avatarUrl, Instant now) {
    this.publicSlug = CreatorSlug.normalize(slug);
    this.publicDisplayName = requiredBounded(displayName, DISPLAY_NAME_MAX);
    this.bio = bounded(bio, BIO_MAX);
    this.avatarUrl = boundedHttpUrl(avatarUrl, AVATAR_URL_MAX);
    this.updatedAt = Objects.requireNonNull(now, "now is required");
  }

  public boolean verify(Instant now) {
    if (verified) {
      return false;
    }
    verified = true;
    updatedAt = Objects.requireNonNull(now, "now is required");
    return true;
  }

  public boolean unverify(Instant now) {
    if (!verified) {
      return false;
    }
    verified = false;
    updatedAt = Objects.requireNonNull(now, "now is required");
    return true;
  }

  public UUID userId() {
    return userId;
  }

  public String publicSlug() {
    return publicSlug;
  }

  public String publicDisplayName() {
    return publicDisplayName;
  }

  public String bio() {
    return bio;
  }

  public String avatarUrl() {
    return avatarUrl;
  }

  public boolean verified() {
    return verified;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  private static String requiredBounded(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("value is required");
    }
    return bounded(value, maxLength);
  }

  private static String bounded(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength);
  }

  private static String boundedHttpUrl(String value, int maxLength) {
    String boundedValue = bounded(value, maxLength);
    if (boundedValue == null) {
      return null;
    }
    try {
      URI uri = new URI(boundedValue);
      String scheme = uri.getScheme();
      if (scheme == null
          || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
          || !StringUtils.hasText(uri.getHost())) {
        throw new IllegalArgumentException("avatarUrl must be an absolute HTTP(S) URL");
      }
      return boundedValue;
    } catch (URISyntaxException exception) {
      throw new IllegalArgumentException("avatarUrl must be an absolute HTTP(S) URL", exception);
    }
  }
}
