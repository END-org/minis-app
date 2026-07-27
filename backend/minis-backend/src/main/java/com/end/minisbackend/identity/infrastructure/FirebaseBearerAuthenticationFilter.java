package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.observability.SafeIdentityFingerprint;
import com.end.minisbackend.shared.security.AuthenticatedRequestLogContext;
import com.end.minisbackend.shared.security.CorrelationId;
import com.end.minisbackend.shared.security.JsonAuthenticationEntryPoint;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseBearerAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(FirebaseBearerAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String FIREBASE_AUTHENTICATION_TYPE = "firebase";
  private static final String SUSPENDED_STATUS = "SUSPENDED";
  private static final String DISABLED_STATUS = "DISABLED";

  private final FirebaseTokenVerifier tokenVerifier;
  private final LocalUserIdentityGateway localUserIdentityGateway;
  private final JsonAuthenticationEntryPoint authenticationEntryPoint;

  FirebaseBearerAuthenticationFilter(
      FirebaseTokenVerifier tokenVerifier,
      LocalUserIdentityGateway localUserIdentityGateway,
      JsonAuthenticationEntryPoint authenticationEntryPoint) {
    this.tokenVerifier = tokenVerifier;
    this.localUserIdentityGateway = localUserIdentityGateway;
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      authenticateIfBearerTokenIsPresent(request);
      filterChain.doFilter(request, response);
    } catch (AuthenticationException exception) {
      logAuthenticationFailure(request, exception);
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(request, response, exception);
    } finally {
      clearAuthenticationMdc();
    }
  }

  private void authenticateIfBearerTokenIsPresent(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      return;
    }
    String token = bearerToken(authorization);
    VerifiedFirebaseIdentity identity = tokenVerifier.verify(token);
    rejectUnverifiedPasswordIdentity(identity);
    Optional<LocalUserIdentity> localUser =
        localUserIdentityGateway.findByFirebaseUid(identity.firebaseUid());
    localUser.ifPresent(FirebaseBearerAuthenticationFilter::rejectInactive);
    MinisPrincipal principal =
        localUser
            .map(user -> new MinisPrincipal(identity, user.id(), user.role(), user.status()))
            .orElseGet(() -> new MinisPrincipal(identity, null, null, null));
    SecurityContextHolder.getContext().setAuthentication(new MinisAuthenticationToken(principal));
    storeAuthenticationContext(request, principal);
    logAuthenticationSuccess(request, identity, principal);
  }

  private static String bearerToken(String authorization) {
    if (!authorization.startsWith(BEARER_PREFIX)) {
      throw new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN);
    }
    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty() || token.contains(" ")) {
      throw new MinisAuthenticationException(ApiErrorCode.INVALID_AUTH_TOKEN);
    }
    return token;
  }

  private static void rejectInactive(LocalUserIdentity user) {
    if (SUSPENDED_STATUS.equals(user.status())) {
      throw new MinisAuthenticationException(ApiErrorCode.ACCOUNT_SUSPENDED);
    }
    if (DISABLED_STATUS.equals(user.status())) {
      throw new MinisAuthenticationException(ApiErrorCode.ACCOUNT_DISABLED);
    }
  }

  private static void rejectUnverifiedPasswordIdentity(VerifiedFirebaseIdentity identity) {
    if (identity.isEmailPasswordSignIn() && !identity.emailVerified()) {
      throw new MinisAuthenticationException(ApiErrorCode.EMAIL_NOT_VERIFIED);
    }
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
    MDC.put(AuthenticatedRequestLogContext.MDC_AUTHENTICATION_TYPE, FIREBASE_AUTHENTICATION_TYPE);
    MDC.put(AuthenticatedRequestLogContext.MDC_PROVISIONED, provisioned);
    MDC.put(
        AuthenticatedRequestLogContext.MDC_FIREBASE_UID_FINGERPRINT,
        SafeIdentityFingerprint.firebaseUid(principal.firebaseIdentity().firebaseUid()));
    if (principal.hasProvisionedAccount()) {
      request.setAttribute(
          AuthenticatedRequestLogContext.LOCAL_USER_ID_ATTRIBUTE, principal.userId());
      request.setAttribute(AuthenticatedRequestLogContext.ROLE_ATTRIBUTE, principal.role());
      request.setAttribute(
          AuthenticatedRequestLogContext.ACCOUNT_STATUS_ATTRIBUTE, principal.accountStatus());
      MDC.put(AuthenticatedRequestLogContext.MDC_LOCAL_USER_ID, principal.userId().toString());
      MDC.put(AuthenticatedRequestLogContext.MDC_ROLE, principal.role());
    }
  }

  private static void logAuthenticationSuccess(
      HttpServletRequest request, VerifiedFirebaseIdentity identity, MinisPrincipal principal) {
    LOGGER.debug(
        "event=firebase_authentication_succeeded correlationId={} provider={} provisioned={} "
            + "localUserId={} role={} status={} firebaseUidFingerprint={}",
        CorrelationId.fromRequest(request),
        valueOrEmpty(identity.signInProvider()),
        principal.hasProvisionedAccount(),
        principal.hasProvisionedAccount() ? principal.userId() : "-",
        valueOrEmpty(principal.role()),
        valueOrEmpty(principal.accountStatus()),
        SafeIdentityFingerprint.firebaseUid(identity.firebaseUid()));
  }

  private static void logAuthenticationFailure(
      HttpServletRequest request, AuthenticationException exception) {
    LOGGER.warn(
        "event=firebase_authentication_failed correlationId={} apiErrorCode={} "
            + "exceptionCategory={} authorizationHeaderPresent={}",
        CorrelationId.fromRequest(request),
        errorCode(exception).name(),
        exception.getClass().getSimpleName(),
        request.getHeader(HttpHeaders.AUTHORIZATION) != null);
  }

  private static ApiErrorCode errorCode(AuthenticationException exception) {
    if (exception instanceof MinisAuthenticationException minisException) {
      return minisException.errorCode();
    }
    return ApiErrorCode.INVALID_AUTH_TOKEN;
  }

  private static void clearAuthenticationMdc() {
    MDC.remove(AuthenticatedRequestLogContext.MDC_AUTHENTICATION_TYPE);
    MDC.remove(AuthenticatedRequestLogContext.MDC_PROVISIONED);
    MDC.remove(AuthenticatedRequestLogContext.MDC_LOCAL_USER_ID);
    MDC.remove(AuthenticatedRequestLogContext.MDC_ROLE);
    MDC.remove(AuthenticatedRequestLogContext.MDC_FIREBASE_UID_FINGERPRINT);
  }

  private static String valueOrEmpty(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
