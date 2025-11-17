package de.dkb.api.codeChallenge.notification.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(RateLimitingInterceptor::class.java)

    // Store buckets per IP address
    private val buckets = ConcurrentHashMap<String, Bucket>()

    // Rate limits per endpoint pattern
    private val rateLimits = mapOf(
        "/register" to Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))), // 10 per minute
        "/notify" to Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))), // 100 per minute
        "/admin" to Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))), // 5 per minute (stricter)
    )

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // Skip rate limiting for actuator endpoints
        if (request.requestURI.startsWith("/actuator")) {
            return true
        }

        val clientIp = getClientIp(request)
        val path = request.requestURI

        // Find matching rate limit for this path
        val bandwidth = rateLimits.entries.find { path.startsWith(it.key) }?.value
            ?: Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1))) // Default: 50 per minute

        // Get or create bucket for this IP
        val bucket = buckets.computeIfAbsent(clientIp) {
            Bucket.builder()
                .addLimit(bandwidth)
                .build()
        }

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            return true
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write("""{"error": "Rate limit exceeded. Please try again later."}""")
            return false
        }
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // Check X-Forwarded-For header (for proxies/load balancers)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (xForwardedFor != null && xForwardedFor.isNotEmpty()) {
            return xForwardedFor.split(",").first().trim()
        }

        // Check X-Real-IP header
        val xRealIp = request.getHeader("X-Real-IP")
        if (xRealIp != null && xRealIp.isNotEmpty()) {
            return xRealIp
        }

        // Fallback to remote address
        return request.remoteAddr ?: "unknown"
    }
}

