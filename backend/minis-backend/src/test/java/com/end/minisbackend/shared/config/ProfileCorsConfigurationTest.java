package com.end.minisbackend.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class ProfileCorsConfigurationTest {
  @Test
  void localDefaultResolvesToViteOriginAndIgnoresDemoOrigin() throws IOException {
    String expression = allowedOriginsExpression("application-local.yml");
    MockEnvironment environment =
        new MockEnvironment().withProperty("MINIS_DEMO_ALLOWED_ORIGINS", "http://localhost:8088");

    String resolved = environment.resolvePlaceholders(expression);

    assertThat(expression).contains("MINIS_LOCAL_ALLOWED_ORIGINS");
    assertThat(expression).doesNotContain("MINIS_ALLOWED_ORIGINS");
    assertThat(new MinisHttpProperties(List.of(resolved)).allowedOrigins())
        .containsExactly("http://localhost:5173");
  }

  @Test
  void demoDefaultResolvesToDemoProxyOriginWithoutUsingLocalVariable() throws IOException {
    String expression = allowedOriginsExpression("application-demo.yml");
    MockEnvironment environment = new MockEnvironment();

    String resolved = environment.resolvePlaceholders(expression);

    assertThat(expression).contains("MINIS_DEMO_ALLOWED_ORIGINS");
    assertThat(expression).doesNotContain("MINIS_ALLOWED_ORIGINS");
    assertThat(expression).doesNotContain("MINIS_LOCAL_ALLOWED_ORIGINS");
    assertThat(new MinisHttpProperties(List.of(resolved)).allowedOrigins())
        .containsExactly("http://localhost:8088");
  }

  @Test
  void productionCorsIsRestrictiveUnlessExplicitlyConfigured() throws IOException {
    String expression = allowedOriginsExpression("application-prod.yml");
    MockEnvironment environment = new MockEnvironment();

    String resolved = environment.resolvePlaceholders(expression);

    assertThat(expression).contains("MINIS_PROD_ALLOWED_ORIGINS");
    assertThat(new MinisHttpProperties(List.of(resolved)).allowedOrigins()).isEmpty();
  }

  private static String allowedOriginsExpression(String resourceName) throws IOException {
    PropertySource<?> propertySource =
        new YamlPropertySourceLoader()
            .load(resourceName, new ClassPathResource(resourceName))
            .getFirst();
    return Objects.toString(propertySource.getProperty("minis.http.allowed-origins"));
  }
}
