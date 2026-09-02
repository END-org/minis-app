package com.end.minisbackend.identity.infrastructure.security;

import com.end.minisbackend.identity.application.model.MinisPrincipal;
import com.end.minisbackend.identity.application.model.ProvisionedUserAccount;
import com.end.minisbackend.identity.application.port.BootstrapAuthenticationContextRefresher;
import com.end.minisbackend.identity.domain.model.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.observability.SafeIdentityFingerprint;
import com.end.minisbackend.shared.security.AuthenticatedRequestLogContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class SpringSecurityBootstrapAuthenticationContextRefresher
    implements BootstrapAuthenticationContextRefresher {
  private static final String FIREBASE_AUTHENTICATION_TYPE = "firebase";

  @Override
  public void refresh(VerifiedFirebaseIdentity identity, ProvisionedUserAccount account) {
    MinisPrincipal principal =
        new MinisPrincipal(identity, account.id(), account.role(), account.status());
    SecurityContextHolder.getContext().setAuthentication(new MinisAuthenticationToken(principal));
    currentRequest().ifPresent(request -> storeAuthenticationContext(request, principal));
  }

  private static java.util.Optional<HttpServletRequest> currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return java.util.Optional.of(servletRequestAttributes.getRequest());
    }
    return java.util.Optional.empty();
  }

  private static void storeAuthenticationContext(
      HttpServletRequest request, MinisPrincipal principal) {
    String provisioned = Boolean.toString(principal.hasProvisionedAccount());
    request.setAttribute(AuthenticatedRequestLogContext.AUTHENTICATED_ATTRIBUTE, true);
    request.setAttribute(
        AuthenticatedRequestLogContext.AUTHENTICATION_TYPE_ATTRIBUTE, FIREBASE_AUTHENTICATION_TYPE);
    request.setAttribute(AuthenticatedRequestLogContext.PROVISIONED_ATTRIBUTE, provisioned);
    request.setAttribute(
        AuthenticatedRequestLogContext.FIREBASE_UID_FINGERPRINT_ATTRIBUTE,
        SafeIdentityFingerprint.firebaseUid(principal.firebaseIdentity().firebaseUid()));
    request.setAttribute(
        AuthenticatedRequestLogContext.LOCAL_USER_ID_ATTRIBUTE, principal.userId());
    request.setAttribute(AuthenticatedRequestLogContext.ROLE_ATTRIBUTE, principal.role());
    request.setAttribute(
        AuthenticatedRequestLogContext.ACCOUNT_STATUS_ATTRIBUTE, principal.accountStatus());
    MDC.put(AuthenticatedRequestLogContext.MDC_AUTHENTICATION_TYPE, FIREBASE_AUTHENTICATION_TYPE);
    MDC.put(AuthenticatedRequestLogContext.MDC_PROVISIONED, provisioned);
    MDC.put(AuthenticatedRequestLogContext.MDC_LOCAL_USER_ID, principal.userId().toString());
    MDC.put(AuthenticatedRequestLogContext.MDC_ROLE, principal.role());
    MDC.put(
        AuthenticatedRequestLogContext.MDC_FIREBASE_UID_FINGERPRINT,
        SafeIdentityFingerprint.firebaseUid(principal.firebaseIdentity().firebaseUid()));
  }
}
