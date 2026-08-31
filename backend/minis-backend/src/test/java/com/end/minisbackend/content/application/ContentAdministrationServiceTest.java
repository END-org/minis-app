package com.end.minisbackend.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.TestTransactionManager;
import com.end.minisbackend.content.domain.ContentCategory;
import com.end.minisbackend.content.domain.ContentItem;
import com.end.minisbackend.content.domain.ContentStatus;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentAdministrationServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void adminRemovesPublishedContentAndRepeatedRemovalIsIdempotent() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    ContentItem content = contentItem();
    store.saveAndFlush(content);
    ContentAdministrationService service = service(store);

    service.remove(admin(), content.id(), "event cleanup");
    service.remove(admin(), content.id(), "event cleanup");

    ContentItem stored = store.findById(content.id()).orElseThrow();
    assertThat(stored.status()).isEqualTo(ContentStatus.REMOVED);
    assertThat(stored.removedAt()).isEqualTo(Instant.parse("2026-07-17T12:00:00Z"));
    assertThat(stored.removalReason()).isEqualTo("event cleanup");
    assertThat(store.findPublishedItemsAfter(null, 100)).isEmpty();
  }

  @Test
  void viewerCannotRemoveContent() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    store.saveAndFlush(contentItem());
    ContentAdministrationService service = service(store);

    assertThatThrownBy(
            () ->
                service.remove(
                    new MinisPrincipal(
                        TestIdentities.googleIdentity("viewer"),
                        UUID.randomUUID(),
                        "VIEWER",
                        "ACTIVE"),
                    store.all().get(0).id(),
                    "event cleanup"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void removalReasonIsBoundedAndRequired() {
    InMemoryContentCommandStore store = new InMemoryContentCommandStore();
    ContentItem content = contentItem();
    store.saveAndFlush(content);
    ContentAdministrationService service = service(store);

    assertThatThrownBy(() -> service.remove(admin(), content.id(), " "))
        .isInstanceOf(IllegalArgumentException.class);
    ContentItem secondContent = contentItem();
    store.saveAndFlush(secondContent);
    assertThatThrownBy(() -> service.remove(admin(), secondContent.id(), "a".repeat(301)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static ContentAdministrationService service(InMemoryContentCommandStore store) {
    return new ContentAdministrationService(store, new TestTransactionManager(), CLOCK);
  }

  private static ContentItem contentItem() {
    return ContentItem.publishImported(
        UUID.randomUUID(),
        "Event title",
        "Event description",
        ContentCategory.EDUCATION,
        "content/imports/test.mp4",
        "video/mp4",
        1024L,
        1000L,
        UUID.randomUUID().toString(),
        Instant.parse("2026-07-17T12:00:00Z"));
  }

  private static MinisPrincipal admin() {
    return new MinisPrincipal(
        TestIdentities.googleIdentity("admin"), UUID.randomUUID(), "ADMIN", "ACTIVE");
  }
}
