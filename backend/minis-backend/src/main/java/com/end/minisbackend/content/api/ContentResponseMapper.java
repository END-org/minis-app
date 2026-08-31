package com.end.minisbackend.content.api;

import com.end.minisbackend.content.application.ContentPage;
import com.end.minisbackend.content.application.PublicContentItem;

final class ContentResponseMapper {
  private ContentResponseMapper() {}

  static ContentPageResponse toResponse(ContentPage page) {
    return new ContentPageResponse(
        page.items().stream().map(ContentResponseMapper::toResponse).toList(),
        page.nextCursor(),
        page.limit());
  }

  static ContentResponse toResponse(PublicContentItem item) {
    return new ContentResponse(
        item.id(),
        new ContentCreatorResponse(
            item.creator().userId(),
            item.creator().slug(),
            item.creator().displayName(),
            item.creator().avatarUrl(),
            item.creator().verified()),
        item.title(),
        item.description(),
        item.category(),
        item.categoryLabel(),
        new ContentMediaResponse(
            item.media().url(),
            item.media().expiresAt(),
            item.media().contentType(),
            item.media().sizeBytes(),
            item.media().durationMs()),
        item.publishedAt(),
        item.likeCount(),
        item.liked(),
        item.saved(),
        item.following());
  }
}
