package de.dkb.api.codeChallenge.notification.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        logger.error("Illegal argument exception: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (ex.message ?: "Invalid request")))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        logger.warn("Validation failed: {}", ex.message)
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "Validation failed", "details" to errors.toString()))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<Map<String, String>> {
        logger.warn("Type mismatch exception: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "Invalid parameter type: ${ex.name}"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Unexpected error occurred: {}", ex.message, ex)
        // In production, you might want to hide internal error details
        // For now, we'll log the full exception but return a generic message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "An unexpected error occurred. Please try again later."))
    }
}

