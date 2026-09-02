package com.end.minisbackend.identity.application.port;

import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;

/** Verifies a Firebase ID token and returns only trusted identity claims. */
public interface FirebaseTokenVerifier {
  VerifiedFirebaseIdentity verify(String idToken);
}
