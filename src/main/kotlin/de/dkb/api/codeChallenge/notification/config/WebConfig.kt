package de.dkb.api.codeChallenge.notification.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loggingInterceptor: LoggingInterceptor,
    private val rateLimitingInterceptor: RateLimitingInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Rate limiting should be checked first
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**") // Exclude actuator endpoints from rate limiting

        // Then logging
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**") // Exclude actuator endpoints from logging
    }
}

