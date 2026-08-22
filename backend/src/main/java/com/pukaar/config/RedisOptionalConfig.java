package com.pukaar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Redis is available for cache/session scaling. App boots with Caffeine if Redis is down in local/dev.
 */
@Configuration
@ConditionalOnProperty(name = "pukaar.redis.enabled", havingValue = "true")
public class RedisOptionalConfig {
}
