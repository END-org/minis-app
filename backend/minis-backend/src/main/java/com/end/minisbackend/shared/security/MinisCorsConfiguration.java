package com.end.minisbackend.shared.security;

import com.end.minisbackend.shared.config.MinisHttpProperties;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
class MinisCorsConfiguration {
  @Bean
  CorsConfigurationSource minisCorsConfigurationSource(MinisHttpProperties properties) {
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
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, CorrelationId.HEADER_NAME));
    configuration.setExposedHeaders(List.of(CorrelationId.HEADER_NAME));
    configuration.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  FilterRegistrationBean<CorsFilter> minisCorsFilter(
      CorsConfigurationSource minisCorsConfigurationSource) {
    CorsFilter corsFilter = new CorsFilter(minisCorsConfigurationSource);
    corsFilter.setCorsProcessor(new LoggingCorsProcessor());
    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
    return registration;
  }
}
