package com.end.minisbackend.content.api.mapper;

import com.end.minisbackend.content.api.dto.feed.ContentCreatorResponse;
import com.end.minisbackend.content.api.dto.feed.ContentMediaResponse;
import com.end.minisbackend.content.api.dto.feed.ContentPageResponse;
import com.end.minisbackend.content.api.dto.feed.ContentResponse;
import com.end.minisbackend.content.application.model.feed.ContentPage;
import com.end.minisbackend.content.application.model.feed.PublicContentItem;

public final class ContentResponseMapper {
  private ContentResponseMapper() {}

  public static ContentPageResponse toResponse(ContentPage page) {
    return new ContentPageResponse(
        page.items().stream().map(ContentResponseMapper::toResponse).toList(),
        page.nextCursor(),
        page.limit());
  }

  public static ContentResponse toResponse(PublicContentItem item) {
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
