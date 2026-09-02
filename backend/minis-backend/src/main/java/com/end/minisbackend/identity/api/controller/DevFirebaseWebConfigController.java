package com.end.minisbackend.identity.api.controller;

import com.end.minisbackend.identity.api.dto.FirebaseWebConfigResponse;
import com.end.minisbackend.identity.application.port.FirebaseWebConfigProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "demo"})
@RestController
@RequestMapping("/api/v1/dev")
class DevFirebaseWebConfigController {
  private final FirebaseWebConfigProvider properties;

  DevFirebaseWebConfigController(FirebaseWebConfigProvider properties) {
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
