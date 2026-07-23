package com.end.minisbackend.identity.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("!test")
class FirebaseAdminConfiguration {
  @Bean
  FirebaseApp firebaseApp(FirebaseAdminProperties properties) {
    if (!StringUtils.hasText(properties.projectId())) {
      throw new IllegalStateException("FIREBASE_PROJECT_ID is required for Firebase Admin.");
    }

    Optional<FirebaseApp> existingDefaultApp =
        FirebaseApp.getApps().stream()
            .filter(app -> FirebaseApp.DEFAULT_APP_NAME.equals(app.getName()))
            .findFirst();
    if (existingDefaultApp.isPresent()) {
      verifyProject(properties, existingDefaultApp.get());
      return existingDefaultApp.get();
    }

    try {
      FirebaseOptions options =
          FirebaseOptions.builder()
              .setCredentials(GoogleCredentials.getApplicationDefault())
              .setProjectId(properties.projectId())
              .build();
      return FirebaseApp.initializeApp(options);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Firebase Admin credentials are required. Set GOOGLE_APPLICATION_CREDENTIALS to a file outside Git.",
          exception);
    }
  }

  private static void verifyProject(FirebaseAdminProperties properties, FirebaseApp firebaseApp) {
    String existingProjectId = firebaseApp.getOptions().getProjectId();
    if (StringUtils.hasText(existingProjectId)
        && !properties.projectId().equals(existingProjectId)) {
      throw new IllegalStateException(
          "Existing FirebaseApp project does not match FIREBASE_PROJECT_ID.");
    }
  }
}
