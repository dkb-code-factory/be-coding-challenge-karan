package de.dkb.api.codeChallenge.notification.kafka

import de.dkb.api.codeChallenge.notification.NotificationService
import de.dkb.api.codeChallenge.notification.model.NotificationDto
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class NotificationSubscriber(private val notificationService: NotificationService) {

    // For the challenge this is deactivated, but consider this functionality of a heavy use in the real life system
    @KafkaListener(
        topics = ["notifications"],
        groupId = "codechallenge_group",
        autoStartup = "\${kafka.listener.enabled:false}"
    )
    fun consumeNotification(notificationDto: NotificationDto) =
        notificationService.sendNotification(notificationDto)
}