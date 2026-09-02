package com.end.minisbackend.user.infrastructure.config;

import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import com.end.minisbackend.user.application.port.InitialUserRolePolicy;
import com.end.minisbackend.user.domain.enums.UserRole;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
class ConfiguredInitialUserRolePolicy implements InitialUserRolePolicy {
  private final EventAdminBootstrapProperties properties;
  private final Environment environment;
  private final Set<String> adminFirebaseUids;

  ConfiguredInitialUserRolePolicy(
      EventAdminBootstrapProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
    this.adminFirebaseUids = Set.copyOf(properties.firebaseUids());
  }

  @PostConstruct
  void validateProfile() {
    if (properties.enabled() && !environment.acceptsProfiles(Profiles.of("local", "demo"))) {
      throw new IllegalStateException(
          "minis.event-admin.bootstrap.enabled is only allowed with local or demo profiles");
    }
  }

  @Override
  public UserRole roleFor(VerifiedFirebaseIdentity identity) {
    if (properties.enabled() && adminFirebaseUids.contains(identity.firebaseUid())) {
      return UserRole.ADMIN;
    }
    return UserRole.VIEWER;
  }
}
