package com.end.minisbackend.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfiguration {
  @Bean
  Clock minisClock() {
    return Clock.systemUTC();
  }
}
