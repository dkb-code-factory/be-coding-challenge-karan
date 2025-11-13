package de.dkb.api.codeChallenge.notification

import de.dkb.api.codeChallenge.notification.model.NotificationDto
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(private val userRepository: UserRepository) {

    fun registerUser(user: User) = userRepository.save(user)

    fun sendNotification(notificationDto: NotificationDto) =
        userRepository.findById(notificationDto.userId)
            .filter { it.notifications.contains(notificationDto.notificationType) }
            .ifPresent { // Logic to send notification to user
                println(
                    "Sending notification of type ${notificationDto.notificationType}" +
                            " to user ``````${it.id}: ${notificationDto.message}"
                )
            }
}