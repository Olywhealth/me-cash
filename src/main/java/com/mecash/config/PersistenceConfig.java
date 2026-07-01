package com.mecash.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing (drives {@code @CreatedDate}) and binds {@link MecashProperties}.
 */
@Configuration
@EnableJpaAuditing
@EnableConfigurationProperties(MecashProperties.class)
public class PersistenceConfig {
}
