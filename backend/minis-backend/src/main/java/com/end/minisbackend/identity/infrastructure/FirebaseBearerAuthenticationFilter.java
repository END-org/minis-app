package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.identity.application.FirebaseTokenVerifier;
import com.end.minisbackend.identity.application.LocalUserIdentity;
import com.end.minisbackend.identity.application.LocalUserIdentityGateway;
import com.end.minisbackend.identity.application.MinisPrincipal;
import com.end.minisbackend.identity.domain.VerifiedFirebaseIdentity;
import com.end.minisbackend.shared.error.ApiErrorCode;
import com.end.minisbackend.shared.security.JsonAuthenticationEntryPoint;
import com.end.minisbackend.shared.security.MinisAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseBearerAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER_PREFIX = "Bearer ";
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
      SecurityContextHolder.clearContext();
      authenticationEntryPoint.commence(request, response, exception);
    }
  }

  private void authenticateIfBearerTokenIsPresent(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      return;
    }
    String token = bearerToken(authorization);
    VerifiedFirebaseIdentity identity = tokenVerifier.verify(token);
    Optional<LocalUserIdentity> localUser =
        localUserIdentityGateway.findByFirebaseUid(identity.firebaseUid());
    localUser.ifPresent(FirebaseBearerAuthenticationFilter::rejectInactive);
    MinisPrincipal principal =
        localUser
            .map(user -> new MinisPrincipal(identity, user.id(), user.role(), user.status()))
            .orElseGet(() -> new MinisPrincipal(identity, null, null, null));
    SecurityContextHolder.getContext().setAuthentication(new MinisAuthenticationToken(principal));
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
}
