package de.dkb.api.codeChallenge.notification.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("notificationTypeCategories")
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1000) // Maximum number of entries in cache
                .expireAfterWrite(1, TimeUnit.HOURS) // Cache expires after 1 hour
                .recordStats(), // Enable cache statistics
        )
        return cacheManager
    }
}

