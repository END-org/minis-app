package com.end.minisbackend.identity.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
  ResponseEntity<Map<String, String>> firebaseWebConfig() {
    Map<String, String> config = new LinkedHashMap<>();
    putIfPresent(config, "apiKey", properties.apiKey());
    putIfPresent(config, "authDomain", properties.authDomain());
    putIfPresent(config, "projectId", properties.projectId());
    putIfPresent(config, "appId", properties.appId());
    putIfPresent(config, "storageBucket", properties.storageBucket());
    return ResponseEntity.ok(config);
  }

  private static void putIfPresent(Map<String, String> config, String key, String value) {
    if (StringUtils.hasText(value)) {
      config.put(key, value);
    }
  }
}
