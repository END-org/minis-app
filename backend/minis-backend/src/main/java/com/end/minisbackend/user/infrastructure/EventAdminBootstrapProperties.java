package com.end.minisbackend.user.infrastructure;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "minis.event-admin.bootstrap")
record EventAdminBootstrapProperties(boolean enabled, List<String> firebaseUids) {
  EventAdminBootstrapProperties {
    firebaseUids =
        firebaseUids == null
            ? List.of()
            : List.copyOf(
                firebaseUids.stream().filter(StringUtils::hasText).map(String::trim).toList());
  }

  @Override
  public List<String> firebaseUids() {
    return List.copyOf(firebaseUids);
  }
}
