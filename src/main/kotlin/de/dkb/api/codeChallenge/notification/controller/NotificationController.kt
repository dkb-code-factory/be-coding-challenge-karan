package de.dkb.api.codeChallenge.notification.controller

import de.dkb.api.codeChallenge.notification.dto.AddNotificationTypeDto
import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.service.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class NotificationController(private val notificationService: NotificationService) {

    @PostMapping("/register")
    fun registerUser(@RequestBody user: User): ResponseEntity<Map<String, Any>> {
        val result = notificationService.registerUser(user)
        return when {
            result.wasCreated -> {
                ResponseEntity.status(HttpStatus.CREATED).body(
                    mapOf(
                        "message" to "User registered successfully",
                        "user" to result.user,
                    ),
                )
            }
            // We could create a separate endpoint for updating explicitly, but
            // since the endpoint is already used by many clients, it doesn't make sense to change it now
            result.wasUpdated -> {
                ResponseEntity.ok(
                    mapOf(
                        "message" to "User updated successfully",
                        "user" to result.user,
                    ),
                )
            }

            else -> {
                ResponseEntity.ok(
                    mapOf(
                        "message" to "User already exists with the same data",
                        "user" to result.user,
                    ),
                )
            }
        }
    }

    @PostMapping("/notify")
    fun sendNotification(@RequestBody notificationDto: NotificationDto): ResponseEntity<Map<String, String>> {
        notificationService.sendNotification(notificationDto)
        return ResponseEntity.ok(mapOf("message" to "Notification sent successfully"))
    }

    @PostMapping("/admin/notification-types")
    fun addNotificationType(@RequestBody request: AddNotificationTypeDto): ResponseEntity<Map<String, String>> {
        notificationService.addNotificationType(request.notificationType, request.category)
        return ResponseEntity.ok(mapOf("message" to "Notification type '${request.notificationType}' added to category '${request.category}'"))
    }
}

