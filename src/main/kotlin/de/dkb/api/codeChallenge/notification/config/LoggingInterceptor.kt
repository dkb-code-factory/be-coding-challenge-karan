package de.dkb.api.codeChallenge.notification.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute("startTime", startTime)
        logger.info(
            "Incoming request: method={}, uri={}, remoteAddr={}",
            request.method,
            request.requestURI,
            request.remoteAddr,
        )
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?,
    ) {
        val startTime = request.getAttribute("startTime") as? Long ?: return
        val duration = System.currentTimeMillis() - startTime
        logger.info(
            "Request completed: method={}, uri={}, status={}, duration={}ms",
            request.method,
            request.requestURI,
            response.status,
            duration,
        )
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (ex != null) {
            logger.error(
                "Request failed: method={}, uri={}, status={}, error={}",
                request.method,
                request.requestURI,
                response.status,
                ex.message,
                ex,
            )
        }
    }
}

