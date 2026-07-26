package com.end.minisbackend.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.end.minisbackend.TestIdentities;
import com.end.minisbackend.user.domain.UserRole;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ConfiguredInitialUserRolePolicyTest {
  @Test
  void configuredAllowlistCanBootstrapAdminInLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    ConfiguredInitialUserRolePolicy policy =
        new ConfiguredInitialUserRolePolicy(
            new EventAdminBootstrapProperties(true, List.of("allowed-admin")), environment);
    policy.validateProfile();

    assertThat(policy.roleFor(TestIdentities.googleIdentity("allowed-admin")))
        .isEqualTo(UserRole.ADMIN);
    assertThat(policy.roleFor(TestIdentities.googleIdentity("ordinary-viewer")))
        .isEqualTo(UserRole.VIEWER);
  }

  @Test
  void configuredAllowlistCanBootstrapAdminInDemoProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("demo");
    ConfiguredInitialUserRolePolicy policy =
        new ConfiguredInitialUserRolePolicy(
            new EventAdminBootstrapProperties(true, List.of("allowed-admin")), environment);
    policy.validateProfile();

    assertThat(policy.roleFor(TestIdentities.googleIdentity("allowed-admin")))
        .isEqualTo(UserRole.ADMIN);
    assertThat(policy.roleFor(TestIdentities.googleIdentity("ordinary-viewer")))
        .isEqualTo(UserRole.VIEWER);
  }

  @Test
  void defaultDisabledBootstrapDoesNotGrantAdmin() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    ConfiguredInitialUserRolePolicy policy =
        new ConfiguredInitialUserRolePolicy(
            new EventAdminBootstrapProperties(false, List.of()), environment);
    policy.validateProfile();

    assertThat(policy.roleFor(TestIdentities.googleIdentity("allowed-admin")))
        .isEqualTo(UserRole.VIEWER);
  }

  @Test
  void disabledBootstrapKeepsAllowlistedUidAsViewer() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    ConfiguredInitialUserRolePolicy policy =
        new ConfiguredInitialUserRolePolicy(
            new EventAdminBootstrapProperties(false, List.of("allowed-admin")), environment);
    policy.validateProfile();

    assertThat(policy.roleFor(TestIdentities.googleIdentity("allowed-admin")))
        .isEqualTo(UserRole.VIEWER);
  }

  @Test
  void adminBootstrapIsRejectedOutsideLocalOrDemoProfiles() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    ConfiguredInitialUserRolePolicy policy =
        new ConfiguredInitialUserRolePolicy(
            new EventAdminBootstrapProperties(true, List.of("allowed-admin")), environment);

    assertThatThrownBy(policy::validateProfile).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void trackedProfilesDoNotEnableAdminBootstrapOrInlineUidDefaults() throws IOException {
    assertSafeTrackedProfile("application.yml");
    assertSafeTrackedProfile("application-local.yml");
    assertSafeTrackedProfile("application-demo.yml");
  }

  private static void assertSafeTrackedProfile(String resourceName) throws IOException {
    String text;
    try (var stream =
        ConfiguredInitialUserRolePolicyTest.class
            .getClassLoader()
            .getResourceAsStream(resourceName)) {
      assertThat(stream).as(resourceName).isNotNull();
      text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(text).doesNotContain("MINIS_EVENT_ADMIN_BOOTSTRAP_ENABLED:true");
    assertThat(text).contains("MINIS_EVENT_ADMIN_BOOTSTRAP_FIREBASE_UIDS:}");
  }
}
