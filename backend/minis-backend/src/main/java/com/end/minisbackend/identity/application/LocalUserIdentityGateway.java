package com.end.minisbackend.identity.application;

import java.util.Optional;

/** Looks up the optional MINIS account attached to a verified Firebase UID. */
public interface LocalUserIdentityGateway {
  Optional<LocalUserIdentity> findByFirebaseUid(String firebaseUid);
}
