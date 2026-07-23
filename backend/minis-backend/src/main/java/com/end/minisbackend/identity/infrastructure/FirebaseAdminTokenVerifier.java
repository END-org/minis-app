package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {
  private static final String SIGN_IN_PROVIDER = "sign_in_provider";

  private final FirebaseAuth firebaseAuth;

  FirebaseAdminTokenVerifier(FirebaseApp firebaseApp) {
    this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
  }

  @Override
  public VerifiedFirebaseIdentity verify(String idToken) {
    try {
      FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
      return toIdentity(token);
    } catch (FirebaseAuthException exception) {
      throw new MinisAuthenticationException(errorCodeFor(exception), exception);
    } catch (IllegalArgumentException exception) {
      throw new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN, exception);
    }
  }

  private static VerifiedFirebaseIdentity toIdentity(FirebaseToken token) {
    return new VerifiedFirebaseIdentity(
        token.getUid(),
        token.getEmail(),
        token.isEmailVerified(),
        token.getName(),
        token.getPicture(),
        signInProvider(token.getClaims()),
        epochSecondClaim(token.getClaims(), "iat"),
        epochSecondClaim(token.getClaims(), "exp"));
  }

  private static String signInProvider(Map<String, Object> claims) {
    Object firebase = claims.get("firebase");
    if (!(firebase instanceof Map<?, ?> firebaseClaims)) {
      return null;
    }
    Object provider = firebaseClaims.get(SIGN_IN_PROVIDER);
    return provider instanceof String value ? value : null;
  }

  private static Instant epochSecondClaim(Map<String, Object> claims, String claimName) {
    Object value = claims.get(claimName);
    if (value instanceof Number number) {
      return Instant.ofEpochSecond(number.longValue());
    }
    return null;
  }

  private static ApiErrorCode errorCodeFor(FirebaseAuthException exception) {
    if (exception.getAuthErrorCode() == null) {
      return ApiErrorCode.INVALID_AUTH_TOKEN;
    }
    String authErrorCode = exception.getAuthErrorCode().name();
    if ("ID_TOKEN_EXPIRED".equals(authErrorCode)) {
      return ApiErrorCode.EXPIRED_AUTH_TOKEN;
    }
    return ApiErrorCode.INVALID_AUTH_TOKEN;
  }
}
