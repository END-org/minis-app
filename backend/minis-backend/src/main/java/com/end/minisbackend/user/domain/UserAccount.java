package com.end.minisbackend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.util.StringUtils;

@Entity
@Table(
    name = "user_accounts",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_user_account_firebase_uid", columnNames = "firebase_uid"),
    indexes = @Index(name = "idx_user_accounts_email", columnList = "email"))
public class UserAccount {
  private static final int FIREBASE_UID_MAX = 128;
  private static final int EMAIL_MAX = 320;
  private static final int DISPLAY_NAME_MAX = 120;
  private static final int AVATAR_URL_MAX = 1024;
  private static final int BIO_MAX = 500;

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(
      name = "firebase_uid",
      nullable = false,
      unique = true,
      length = FIREBASE_UID_MAX,
      updatable = false)
  private String firebaseUid;

  @Column(length = EMAIL_MAX)
  private String email;

  @Column(nullable = false)
  private boolean emailVerified;

  @Column(length = DISPLAY_NAME_MAX)
  private String displayName;

  @Column(length = AVATAR_URL_MAX)
  private String avatarUrl;

  @Column(length = BIO_MAX)
  private String bio;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AccountStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Column(nullable = false)
  private Instant lastAuthenticatedAt;

  protected UserAccount() {}

  private UserAccount(
      UUID id,
      String firebaseUid,
      String email,
      boolean emailVerified,
      String displayName,
      String avatarUrl,
      Instant now) {
    this.id = id;
    this.firebaseUid = firebaseUid;
    this.email = email;
    this.emailVerified = emailVerified;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.role = UserRole.VIEWER;
    this.status = AccountStatus.ACTIVE;
    this.createdAt = now;
    this.updatedAt = now;
    this.lastAuthenticatedAt = now;
  }

  public static UserAccount createViewer(
      String firebaseUid,
      String email,
      boolean emailVerified,
      String displayName,
      String avatarUrl,
      Instant now) {
    String boundedFirebaseUid = requiredBounded(firebaseUid, FIREBASE_UID_MAX);
    String boundedEmail = bounded(email, EMAIL_MAX);
    String boundedDisplayName = bounded(displayName, DISPLAY_NAME_MAX);
    String boundedAvatarUrl = bounded(avatarUrl, AVATAR_URL_MAX);
    Instant createdAt = Objects.requireNonNull(now, "now is required");
    return new UserAccount(
        UUID.randomUUID(),
        boundedFirebaseUid,
        boundedEmail,
        emailVerified,
        boundedDisplayName,
        boundedAvatarUrl,
        createdAt);
  }

  public void synchronizeTrustedIdentity(
      String email, boolean emailVerified, String displayName, String avatarUrl, Instant now) {
    this.email = bounded(email, EMAIL_MAX);
    this.emailVerified = emailVerified;
    if (!StringUtils.hasText(this.displayName) && StringUtils.hasText(displayName)) {
      this.displayName = bounded(displayName, DISPLAY_NAME_MAX);
    }
    if (!StringUtils.hasText(this.avatarUrl) && StringUtils.hasText(avatarUrl)) {
      this.avatarUrl = bounded(avatarUrl, AVATAR_URL_MAX);
    }
    this.lastAuthenticatedAt = now;
    this.updatedAt = now;
  }

  public void suspend() {
    this.status = AccountStatus.SUSPENDED;
  }

  public void disable() {
    this.status = AccountStatus.DISABLED;
  }

  public void updateProfile(String displayName, String avatarUrl, String bio, Instant now) {
    this.displayName = bounded(displayName, DISPLAY_NAME_MAX);
    this.avatarUrl = bounded(avatarUrl, AVATAR_URL_MAX);
    this.bio = bounded(bio, BIO_MAX);
    this.updatedAt = now;
  }

  public UUID id() {
    return id;
  }

  public String firebaseUid() {
    return firebaseUid;
  }

  public String email() {
    return email;
  }

  public boolean emailVerified() {
    return emailVerified;
  }

  public String displayName() {
    return displayName;
  }

  public String avatarUrl() {
    return avatarUrl;
  }

  public String bio() {
    return bio;
  }

  public UserRole role() {
    return role;
  }

  public AccountStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public Instant lastAuthenticatedAt() {
    return lastAuthenticatedAt;
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
}
