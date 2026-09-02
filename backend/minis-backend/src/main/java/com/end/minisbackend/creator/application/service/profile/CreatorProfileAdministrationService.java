package com.end.minisbackend.creator.application.service.profile;

import com.end.minisbackend.creator.application.model.CreatorProfileChange;
import com.end.minisbackend.creator.application.model.CreatorProfileView;
import com.end.minisbackend.creator.application.model.GrantCreatorProfileCommand;
import com.end.minisbackend.creator.application.port.CreatorProfileStore;
import com.end.minisbackend.creator.domain.model.CreatorProfile;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CreatorProfileAdministrationService {
  private final CreatorProfileStore creatorProfileStore;

  public CreatorProfileAdministrationService(CreatorProfileStore creatorProfileStore) {
    this.creatorProfileStore = creatorProfileStore;
  }

  public CreatorProfileChange ensureProfileForGrant(
      UUID userId, GrantCreatorProfileCommand command, Instant now) {
    try {
      CreatorProfile profile =
          creatorProfileStore
              .findByUserId(userId)
              .map(
                  existing -> {
                    existing.updateGrantProfile(
                        command.slug(),
                        command.displayName(),
                        command.bio(),
                        command.avatarUrl(),
                        now);
                    return existing;
                  })
              .orElseGet(
                  () ->
                      CreatorProfile.create(
                          userId,
                          command.slug(),
                          command.displayName(),
                          command.bio(),
                          command.avatarUrl(),
                          now));
      return new CreatorProfileChange(
          CreatorProfileMapper.toView(creatorProfileStore.saveAndFlush(profile)), true);
    } catch (IllegalArgumentException exception) {
      throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception);
    } catch (DataIntegrityViolationException exception) {
      throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT, exception);
    }
  }

  public CreatorProfileChange verify(UUID userId, Instant now) {
    CreatorProfile profile = requiredProfile(userId);
    boolean changed = profile.verify(now);
    if (changed) {
      creatorProfileStore.saveAndFlush(profile);
    }
    return new CreatorProfileChange(CreatorProfileMapper.toView(profile), changed);
  }

  public CreatorProfileChange unverify(UUID userId, Instant now) {
    CreatorProfile profile = requiredProfile(userId);
    boolean changed = profile.unverify(now);
    if (changed) {
      creatorProfileStore.saveAndFlush(profile);
    }
    return new CreatorProfileChange(CreatorProfileMapper.toView(profile), changed);
  }

  public Map<UUID, CreatorProfileView> profilesByUserIds(List<UUID> userIds) {
    return creatorProfileStore.findByUserIds(userIds).stream()
        .collect(Collectors.toMap(CreatorProfileView::userId, Function.identity()));
  }

  public Optional<CreatorProfileView> profileByUserId(UUID userId) {
    return creatorProfileStore.findByUserId(userId).map(CreatorProfileMapper::toView);
  }

  private CreatorProfile requiredProfile(UUID userId) {
    return creatorProfileStore
        .findByUserId(userId)
        .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
  }
}
