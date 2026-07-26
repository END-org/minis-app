package com.end.minisbackend.identity.api;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "demo"})
@RestController
@RequestMapping("/api/v1/dev")
class DevFirebaseWebConfigController {
  private final FirebaseWebConfigProperties properties;

  DevFirebaseWebConfigController(FirebaseWebConfigProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/firebase-web-config")
  ResponseEntity<FirebaseWebConfigResponse> firebaseWebConfig() {
    return ResponseEntity.ok(
        new FirebaseWebConfigResponse(
            properties.apiKey(),
            properties.authDomain(),
            properties.projectId(),
            properties.appId(),
            properties.storageBucket()));
  }
}
