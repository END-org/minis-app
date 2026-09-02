package com.end.minisbackend.identity.infrastructure.security;

import com.end.minisbackend.identity.application.model.MinisPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

final class MinisAuthenticationToken extends AbstractAuthenticationToken {
  private static final long serialVersionUID = 1L;

  private final MinisPrincipal principal;

  MinisAuthenticationToken(MinisPrincipal principal) {
    super(authoritiesFor(principal));
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public MinisPrincipal getPrincipal() {
    return principal;
  }

  private static Collection<GrantedAuthority> authoritiesFor(MinisPrincipal principal) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("FIREBASE_AUTHENTICATED"));
    if (principal.isActiveProvisionedAccount()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.role()));
    }
    return authorities;
  }
}
