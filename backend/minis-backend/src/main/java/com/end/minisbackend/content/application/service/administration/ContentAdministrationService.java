package com.end.minisbackend.content.application.service.administration;

import com.end.minisbackend.content.application.port.ContentCommandStore;
import com.end.minisbackend.content.domain.model.ContentItem;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContentAdministrationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentAdministrationService.class);
  private static final String ADMIN_ROLE = "ADMIN";

  private final ContentCommandStore contentCommandStore;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  public ContentAdministrationService(
      ContentCommandStore contentCommandStore,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.contentCommandStore = contentCommandStore;
    this.transactionManager = transactionManager;
    this.clock = clock;
  }

  public void remove(MinisPrincipal principal, UUID contentId, String reason) {
    requireAdmin(principal);
    boolean changed =
        writeTransaction()
            .execute(
                status -> {
                  ContentItem content =
                      contentCommandStore
                          .findById(contentId)
                          .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
                  boolean removed = content.remove(reason, Instant.now(clock));
                  if (removed) {
                    contentCommandStore.saveAndFlush(content);
                  }
                  return removed;
                });
    LOGGER.info(
        "event=content_removed correlationId={} actorLocalUserId={} contentId={} changed={}",
        correlationId(),
        principal.userId(),
        contentId,
        changed);
  }

  private TransactionTemplate writeTransaction() {
    return new TransactionTemplate(transactionManager);
  }

  private static void requireAdmin(MinisPrincipal principal) {
    if (principal == null
        || !principal.hasProvisionedAccount()
        || !ADMIN_ROLE.equals(principal.role())) {
      throw new ApiException(ApiErrorCode.ACCESS_DENIED);
    }
  }

  private static String correlationId() {
    String correlationId = MDC.get("correlationId");
    return correlationId == null || correlationId.isBlank() ? "-" : correlationId;
  }
}
