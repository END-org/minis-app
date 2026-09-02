package com.end.minisbackend.content.application.service.feed;

import com.end.minisbackend.content.application.model.feed.CreatorAttribution;
import com.end.minisbackend.content.application.model.feed.PublicContentItem;
import com.end.minisbackend.content.application.model.feed.PublishedContentView;
import com.end.minisbackend.content.application.model.feed.SignedMediaReference;
import com.end.minisbackend.content.application.model.feed.SignedReadUrl;
import com.end.minisbackend.content.application.port.ContentInteractionStore;
import com.end.minisbackend.content.application.port.ContentStorage;
import com.end.minisbackend.content.application.port.ContentStorageException;
import com.end.minisbackend.creator.application.port.CreatorFollowStore;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PublicContentAssembler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PublicContentAssembler.class);

  private final ContentInteractionStore contentInteractionStore;
  private final ContentStorage contentStorage;
  private final CreatorFollowStore creatorFollowStore;

  public PublicContentAssembler(
      ContentInteractionStore contentInteractionStore,
      ContentStorage contentStorage,
      CreatorFollowStore creatorFollowStore) {
    this.contentInteractionStore = contentInteractionStore;
    this.contentStorage = contentStorage;
    this.creatorFollowStore = creatorFollowStore;
  }

  List<PublicContentItem> assemble(List<PublishedContentView> rows, UUID viewerUserId) {
    Map<UUID, ContentSocialState> states = statesByContentId(rows, viewerUserId);
    return rows.stream().map(row -> toPublicItem(row, states.get(row.id()))).toList();
  }

  private PublicContentItem toPublicItem(PublishedContentView row, ContentSocialState state) {
    SignedReadUrl signedReadUrl = signedUrl(row);
    return new PublicContentItem(
        row.id(),
        new CreatorAttribution(
            row.creatorUserId(),
            row.creatorSlug(),
            row.creatorDisplayName(),
            row.creatorAvatarUrl(),
            row.creatorVerified()),
        row.title(),
        row.description(),
        row.category().name(),
        row.category().displayName(),
        new SignedMediaReference(
            signedReadUrl.url().toString(),
            signedReadUrl.expiresAt(),
            row.mediaContentType(),
            row.mediaSizeBytes(),
            row.durationMs()),
        row.publishedAt(),
        state.likeCount(),
        state.liked(),
        state.saved(),
        state.following());
  }

  private Map<UUID, ContentSocialState> statesByContentId(
      List<PublishedContentView> rows, UUID viewerUserId) {
    if (rows.isEmpty()) {
      return Map.of();
    }
    List<UUID> contentIds = rows.stream().map(PublishedContentView::id).toList();
    List<UUID> creatorUserIds =
        rows.stream().map(PublishedContentView::creatorUserId).distinct().toList();
    Map<UUID, Long> likeCounts = contentInteractionStore.likeCountsByContentIds(contentIds);
    Set<UUID> likedContentIds =
        viewerUserId == null
            ? Set.of()
            : contentInteractionStore.likedContentIds(viewerUserId, contentIds);
    Set<UUID> savedContentIds =
        viewerUserId == null
            ? Set.of()
            : contentInteractionStore.savedContentIds(viewerUserId, contentIds);
    Set<UUID> followedCreatorIds =
        viewerUserId == null
            ? Set.of()
            : creatorFollowStore.followedCreatorIds(viewerUserId, creatorUserIds);
    Map<UUID, ContentSocialState> states = new HashMap<>();
    for (PublishedContentView row : rows) {
      states.put(
          row.id(),
          new ContentSocialState(
              likeCounts.getOrDefault(row.id(), 0L),
              likedContentIds.contains(row.id()),
              savedContentIds.contains(row.id()),
              followedCreatorIds.contains(row.creatorUserId())));
    }
    return Map.copyOf(states);
  }

  private SignedReadUrl signedUrl(PublishedContentView row) {
    try {
      return contentStorage.signedReadUrl(row.mediaObjectKey());
    } catch (ContentStorageException exception) {
      LOGGER.warn(
          "event=signed_url_generation_failed contentId={} storageError={}",
          row.id(),
          exception.getClass().getSimpleName());
      throw new ApiException(ApiErrorCode.INTERNAL_ERROR, exception);
    }
  }
}
