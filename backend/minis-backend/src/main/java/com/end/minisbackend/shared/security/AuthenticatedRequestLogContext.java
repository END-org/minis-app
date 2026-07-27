package com.end.minisbackend.shared.security;

/** Request attributes and MDC keys for safe authenticated request logging. */
public final class AuthenticatedRequestLogContext {
  public static final String AUTHENTICATION_TYPE_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".authenticationType";
  public static final String AUTHENTICATED_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".authenticated";
  public static final String PROVISIONED_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".provisioned";
  public static final String LOCAL_USER_ID_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".localUserId";
  public static final String ROLE_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".role";
  public static final String ACCOUNT_STATUS_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".accountStatus";
  public static final String FIREBASE_UID_FINGERPRINT_ATTRIBUTE =
      AuthenticatedRequestLogContext.class.getName() + ".firebaseUidFingerprint";

  public static final String MDC_AUTHENTICATION_TYPE = "authenticationType";
  public static final String MDC_PROVISIONED = "provisioned";
  public static final String MDC_LOCAL_USER_ID = "localUserId";
  public static final String MDC_ROLE = "role";
  public static final String MDC_FIREBASE_UID_FINGERPRINT = "firebaseUidFingerprint";

  private AuthenticatedRequestLogContext() {}
}
