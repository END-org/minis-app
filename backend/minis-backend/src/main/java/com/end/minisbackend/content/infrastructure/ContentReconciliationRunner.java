package com.end.minisbackend.content.infrastructure;

import com.end.minisbackend.content.application.ContentReconciliationService;
import com.end.minisbackend.content.application.ContentReconciliationSummary;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
class ContentReconciliationRunner implements ApplicationRunner {
  private final ContentReconciliationProperties properties;
  private final ContentReconciliationService reconciliationService;
  private final ConfigurableApplicationContext context;

  ContentReconciliationRunner(
      ContentReconciliationProperties properties,
      ContentReconciliationService reconciliationService,
      ConfigurableApplicationContext context) {
    this.properties = properties;
    this.reconciliationService = reconciliationService;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.enabled()) {
      return;
    }
    ContentReconciliationSummary summary =
        reconciliationService.reconcile(
            args.getOptionValues("minis.content.reconciliation.run-id") == null
                ? null
                : args.getOptionValues("minis.content.reconciliation.run-id").get(0));
    SpringApplication.exit(context, summary::exitCode);
  }
}
