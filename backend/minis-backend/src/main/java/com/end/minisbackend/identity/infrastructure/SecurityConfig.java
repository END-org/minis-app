package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.shared.security.JsonAccessDeniedHandler;
import com.end.minisbackend.shared.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      FirebaseBearerAuthenticationFilter firebaseBearerAuthenticationFilter,
      JsonAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler,
      Environment environment)
      throws Exception {
    boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            requests -> {
              requests.requestMatchers("/actuator/health", "/actuator/info").permitAll();
              requests
                  .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                  .permitAll();
              if (localProfile) {
                requests
                    .requestMatchers("/auth-test/**", "/api/v1/dev/firebase-web-config")
                    .permitAll();
              } else {
                requests.requestMatchers("/auth-test/**", "/api/v1/dev/**").denyAll();
              }
              requests.anyRequest().authenticated();
            })
        .addFilterBefore(
            firebaseBearerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
