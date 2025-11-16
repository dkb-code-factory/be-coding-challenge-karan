package de.dkb.api.codeChallenge.notification

import de.dkb.api.codeChallenge.notification.model.NotificationDto
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val notificationCatalog: NotificationCatalog,
) {

    fun registerUser(user: User) = userRepository.save(user)

    fun sendNotification(notificationDto: NotificationDto) =
        userRepository.findById(notificationDto.userId)
            .filter { user ->
                val requestedType = notificationDto.notificationType
                // Exact match still valid
                if (user.notifications.contains(requestedType)) return@filter true

                // Category-based eligibility: any type in the same category qualifies
                val category = notificationCatalog.getCategoryFor(requestedType) ?: return@filter false
                val categoryTypes = notificationCatalog.getTypesFor(category)
                user.notifications.any { it in categoryTypes }
            }
            .ifPresent { // Logic to send notification to user
                println(
                    "Sending notification of type ${notificationDto.notificationType}" +
                            " to user ``````${it.id}: ${notificationDto.message}"
                )
            }
}