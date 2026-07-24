package com.end.minisbackend.identity.infrastructure;

import com.end.minisbackend.shared.config.MinisHttpProperties;
import com.end.minisbackend.shared.security.JsonAccessDeniedHandler;
import com.end.minisbackend.shared.security.JsonAuthenticationEntryPoint;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      FirebaseBearerAuthenticationFilter firebaseBearerAuthenticationFilter,
      JsonAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler,
      MinisHttpProperties httpProperties,
      Environment environment)
      throws Exception {
    boolean developmentProfile = environment.acceptsProfiles(Profiles.of("local", "demo"));
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
              requests
                  .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                  .permitAll();
              requests
                  .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                  .permitAll();
              if (developmentProfile) {
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
    if (httpProperties.hasAllowedOrigins()) {
      http.cors(cors -> cors.configurationSource(corsConfigurationSource(httpProperties)));
    }
    return http.build();
  }

  private static CorsConfigurationSource corsConfigurationSource(MinisHttpProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(
        List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()));
    configuration.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Correlation-ID"));
    configuration.setExposedHeaders(List.of("X-Correlation-ID"));
    configuration.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
