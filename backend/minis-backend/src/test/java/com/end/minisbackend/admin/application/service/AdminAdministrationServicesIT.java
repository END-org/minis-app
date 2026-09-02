package com.end.minisbackend.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.admin.application.model.AdminReasonCommand;
import com.end.minisbackend.admin.application.model.AdminUserPage;
import com.end.minisbackend.admin.application.model.AdminUserView;
import com.end.minisbackend.admin.application.model.GrantCreatorRoleCommand;
import com.end.minisbackend.creator.application.service.read.CreatorProfileQueryService;
import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.error.ApiException;
import com.end.minisbackend.user.application.port.UserAccountStore;
import com.end.minisbackend.user.domain.enums.UserRole;
import com.end.minisbackend.user.domain.model.UserAccount;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AdminAdministrationServicesIT {
  @Autowired private AdminUserQueryService adminUserQueryService;
  @Autowired private CreatorRoleAdministrationService creatorRoleAdministrationService;
  @Autowired private AccountStatusAdministrationService accountStatusAdministrationService;

  @Autowired
  private CreatorVerificationAdministrationService creatorVerificationAdministrationService;

  @Autowired private CreatorProfileQueryService creatorProfileQueryService;
  @Autowired private UserAccountStore userAccountStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void adminCanGrantVerifyRevokeSuspendAndReactivateWithAudit(CapturedOutput output) {
    UserAccount admin = saveUser("phase3-admin", UserRole.ADMIN);
    UserAccount target = saveUser("phase3-target", UserRole.VIEWER);
    MinisPrincipal principal = principal(admin);

    AdminUserView granted =
        creatorRoleAdministrationService.grantCreator(
            principal, target.id(), grantCommand("Event Creator"), "phase3-correlation");

    assertThat(granted.role()).isEqualTo("CREATOR");
    assertThat(granted.creatorProfile().slug()).isEqualTo("event-creator");
    assertThat(auditCount(target.id(), "CREATOR_GRANTED")).isEqualTo(1);

    creatorRoleAdministrationService.grantCreator(
        principal, target.id(), grantCommand("Event Creator"), "phase3-correlation");
    assertThat(auditCount(target.id(), "CREATOR_GRANTED")).isEqualTo(1);

    AdminUserView verified =
        creatorVerificationAdministrationService.verifyCreator(
            principal, target.id(), new AdminReasonCommand("event check"), "verify-correlation");
    assertThat(verified.creatorProfile().verified()).isTrue();
    creatorVerificationAdministrationService.verifyCreator(
        principal, target.id(), new AdminReasonCommand("event check"), "verify-correlation");
    assertThat(auditCount(target.id(), "CREATOR_VERIFIED")).isEqualTo(1);

    AdminUserView revoked =
        creatorRoleAdministrationService.revokeCreator(
            principal, target.id(), new AdminReasonCommand("event complete"), "revoke-correlation");
    assertThat(revoked.role()).isEqualTo("VIEWER");
    assertThat(revoked.creatorProfile()).isNotNull();
    assertThat(auditCount(target.id(), "CREATOR_REVOKED")).isEqualTo(1);

    AdminUserView suspended =
        accountStatusAdministrationService.suspend(
            principal,
            target.id(),
            new AdminReasonCommand("event protection"),
            "suspend-correlation");
    assertThat(suspended.status()).isEqualTo("SUSPENDED");
    assertThat(auditCount(target.id(), "ACCOUNT_SUSPENDED")).isEqualTo(1);

    AdminUserView reactivated =
        accountStatusAdministrationService.reactivate(
            principal, target.id(), new AdminReasonCommand("resolved"), "reactivate-correlation");
    assertThat(reactivated.status()).isEqualTo("ACTIVE");
    assertThat(auditCount(target.id(), "ACCOUNT_REACTIVATED")).isEqualTo(1);
    assertThat(output)
        .contains("event=admin_state_transition_completed")
        .contains("action=CREATOR_GRANTED")
        .contains("action=CREATOR_REVOKED")
        .contains("action=ACCOUNT_SUSPENDED")
        .contains("action=ACCOUNT_REACTIVATED")
        .contains("changed=true")
        .doesNotContain("event protection")
        .doesNotContain("event check")
        .doesNotContain("event complete");
  }

  @Test
  void viewerCannotInvokeAdminUseCases() {
    UserAccount viewer = saveUser("phase3-viewer-admin-blocked", UserRole.VIEWER);

    assertThatThrownBy(() -> adminUserQueryService.users(principal(viewer), null, 0, 20))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.ACCESS_DENIED));
  }

  @Test
  void suspendedCreatorIsHiddenFromPublicCreatorRoutes() {
    UserAccount admin = saveUser("phase3-public-admin", UserRole.ADMIN);
    UserAccount target = saveUser("phase3-public-target", UserRole.VIEWER);
    MinisPrincipal principal = principal(admin);
    creatorRoleAdministrationService.grantCreator(
        principal, target.id(), grantCommand("Public Creator"), "public-correlation");

    assertThat(creatorProfileQueryService.publicCreator("public-creator").slug())
        .isEqualTo("public-creator");

    accountStatusAdministrationService.suspend(
        principal, target.id(), new AdminReasonCommand("protect event"), "suspend-public");

    assertThatThrownBy(() -> creatorProfileQueryService.publicCreator("public-creator"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND));
  }

  @Test
  void adminListCanSearchByCreatorSlug() {
    UserAccount admin = saveUser("phase3-search-admin", UserRole.ADMIN);
    UserAccount target = saveUser("phase3-search-target", UserRole.VIEWER);
    creatorRoleAdministrationService.grantCreator(
        principal(admin), target.id(), grantCommand("Search Creator"), "search-correlation");

    AdminUserPage page = adminUserQueryService.users(principal(admin), "search-creator", 0, 20);

    assertThat(page.users()).extracting(AdminUserView::id).contains(target.id());
  }

  @Test
  void concurrentCreatorGrantsProduceOneLogicalSlugOwner() throws Exception {
    UserAccount admin = saveUser("phase3-concurrent-admin", UserRole.ADMIN);
    UserAccount first = saveUser("phase3-concurrent-first", UserRole.VIEWER);
    UserAccount second = saveUser("phase3-concurrent-second", UserRole.VIEWER);
    MinisPrincipal principal = principal(admin);
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CyclicBarrier barrier = new CyclicBarrier(2);
    List<Callable<String>> tasks = new ArrayList<>();
    tasks.add(() -> grantAfterBarrier(principal, first.id(), barrier));
    tasks.add(() -> grantAfterBarrier(principal, second.id(), barrier));

    try {
      List<Future<String>> futures = executorService.invokeAll(tasks);
      List<String> results =
          futures.stream().map(AdminAdministrationServicesIT::getUnchecked).toList();

      assertThat(results).contains("CREATOR").contains("RESOURCE_CONFLICT");
      assertThat(publicSlugCount("shared-event-creator")).isEqualTo(1);
    } finally {
      executorService.shutdownNow();
    }
  }

  private String grantAfterBarrier(MinisPrincipal principal, UUID targetId, CyclicBarrier barrier)
      throws Exception {
    barrier.await();
    try {
      AdminUserView user =
          creatorRoleAdministrationService.grantCreator(
              principal, targetId, grantCommand("Shared Event Creator"), "concurrent-correlation");
      return user.role();
    } catch (ApiException exception) {
      return exception.errorCode().name();
    }
  }

  private UserAccount saveUser(String firebaseUid, UserRole role) {
    return userAccountStore.saveAndFlush(
        UserAccount.create(
            firebaseUid,
            firebaseUid + "@example.com",
            true,
            firebaseUid,
            null,
            role,
            Instant.parse("2026-07-17T12:00:00Z")));
  }

  private static MinisPrincipal principal(UserAccount account) {
    return new MinisPrincipal(
        TestIdentities.googleIdentity(account.firebaseUid()),
        account.id(),
        account.role().name(),
        account.status().name());
  }

  private static GrantCreatorRoleCommand grantCommand(String slugSource) {
    return new GrantCreatorRoleCommand(slugSource, slugSource, "Event bio", null, "event prep");
  }

  private long auditCount(UUID targetUserId, String actionType) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from event_admin_audit_entries
            where target_user_id = ?
              and action_type = ?
            """,
            Long.class,
            targetUserId,
            actionType);
    return count == null ? 0 : count;
  }

  private long publicSlugCount(String slug) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from creator_profiles where public_slug = ?", Long.class, slug);
    return count == null ? 0 : count;
  }

  private static String getUnchecked(Future<String> future) {
    try {
      return future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for admin operation", exception);
    } catch (java.util.concurrent.ExecutionException exception) {
      throw new AssertionError("Admin operation failed", exception);
    }
  }
}
