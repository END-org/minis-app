package com.end.minisbackend.identity.application.port;

/** Supplies the public Firebase Web SDK values allowed on development surfaces. */
public interface FirebaseWebConfigProvider {
  String apiKey();

  String authDomain();

  String projectId();

  String appId();

  String storageBucket();
}
