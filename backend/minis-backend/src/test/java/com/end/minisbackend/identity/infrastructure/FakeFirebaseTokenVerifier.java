package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.identity.application.port.FirebaseTokenVerifier;
import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Test-only Firebase verifier that keeps automated tests away from real Firebase. */
@Component
@Profile("test")
public class FakeFirebaseTokenVerifier implements FirebaseTokenVerifier {
  public static final String EXPIRED_TOKEN = "expired-token";

  private final Map<String, VerifiedFirebaseIdentity> identitiesByToken = new ConcurrentHashMap<>();

  @Override
  public VerifiedFirebaseIdentity verify(String idToken) {
    if (EXPIRED_TOKEN.equals(idToken)) {
      throw new MinisAuthenticationException(ApiErrorCode.EXPIRED_AUTH_TOKEN);
    }
    VerifiedFirebaseIdentity identity = identitiesByToken.get(idToken);
    if (identity == null) {
      throw new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN);
    }
    return identity;
  }

  public void register(String idToken, VerifiedFirebaseIdentity identity) {
    identitiesByToken.put(idToken, identity);
  }

  public void clear() {
    identitiesByToken.clear();
  }
}
