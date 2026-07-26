package com.end.minisbackend.shared.observability;

import com.end.minisbackend.shared.config.MinisHttpProperties;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Logs a safe summary of effective startup settings that commonly affect event operation. */
@Component
public class StartupConfigurationLogger implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfigurationLogger.class);

  private final Environment environment;
  private final MinisHttpProperties httpProperties;

  public StartupConfigurationLogger(Environment environment, MinisHttpProperties httpProperties) {
    this.environment = environment;
    this.httpProperties = httpProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    LOGGER.info(
        "event=application_configuration_summary activeProfiles={} serverPort={} "
            + "allowedCorsOrigins={} eventAdminBootstrapEnabled={} "
            + "eventAdminBootstrapUidCount={} structuredLoggingMode={}",
        activeProfiles(),
        environment.getProperty("server.port", "8080"),
        httpProperties.allowedOrigins(),
        environment.getProperty("minis.event-admin.bootstrap.enabled", Boolean.class, false),
        eventAdminBootstrapUidCount(),
        environment.getProperty("logging.structured.format.console", "plain"));
  }

  private List<String> activeProfiles() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles.length == 0) {
      return List.of(environment.getDefaultProfiles());
    }
    return List.of(activeProfiles);
  }

  private long eventAdminBootstrapUidCount() {
    String value = environment.getProperty("minis.event-admin.bootstrap.firebase-uids", "");
    if (!StringUtils.hasText(value)) {
      return 0;
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).count();
  }
}
