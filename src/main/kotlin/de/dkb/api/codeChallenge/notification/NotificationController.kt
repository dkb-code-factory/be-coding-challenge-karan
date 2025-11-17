package de.dkb.api.codeChallenge.notification

import de.dkb.api.codeChallenge.notification.model.AddNotificationTypeRequest
import de.dkb.api.codeChallenge.notification.model.NotificationDto
import de.dkb.api.codeChallenge.notification.model.User
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
    fun sendNotification(@RequestBody notificationDto: NotificationDto): ResponseEntity<Map<String, String>> =
        notificationService.sendNotification(notificationDto)

    @PostMapping("/admin/notification-types")
    fun addNotificationType(@RequestBody request: AddNotificationTypeRequest): ResponseEntity<Map<String, String>> {
        return try {
            notificationService.addNotificationType(request.notificationType, request.category)
            ResponseEntity.ok(mapOf("message" to "Notification type '${request.notificationType}' added to category '${request.category}'"))
        } catch (e: IllegalArgumentException) {
            val errorMessage: String = e.message ?: "Invalid request"
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to errorMessage))
        }
    }
}