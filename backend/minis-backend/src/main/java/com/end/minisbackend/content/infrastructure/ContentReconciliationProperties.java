package com.end.minisbackend.content.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minis.content.reconciliation")
record ContentReconciliationProperties(boolean enabled) {}
