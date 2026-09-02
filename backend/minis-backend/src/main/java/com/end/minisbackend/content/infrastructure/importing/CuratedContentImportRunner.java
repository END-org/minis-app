package com.end.minisbackend.content.infrastructure.importing;

import com.end.minisbackend.content.application.model.importing.ContentImportSummary;
import com.end.minisbackend.content.application.service.importing.CuratedContentImportService;
import com.end.minisbackend.content.infrastructure.config.ContentImporterProperties;
import java.nio.file.Path;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
class CuratedContentImportRunner implements ApplicationRunner {
  private final ContentImporterProperties properties;
  private final CuratedContentImportService importService;
  private final ConfigurableApplicationContext context;

  CuratedContentImportRunner(
      ContentImporterProperties properties,
      CuratedContentImportService importService,
      ConfigurableApplicationContext context) {
    this.properties = properties;
    this.importService = importService;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.enabled()) {
      return;
    }
    properties.requireImporterConfigured();
    ContentImportSummary summary =
        importService.importManifest(
            Path.of(properties.manifestPath()),
            Path.of(properties.assetsRoot()),
            args.getOptionValues("minis.content.importer.run-id") == null
                ? null
                : args.getOptionValues("minis.content.importer.run-id").get(0));
    SpringApplication.exit(context, summary::exitCode);
  }
}
