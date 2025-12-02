package de.dkb.api.codeChallenge.notification.kafka

import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.service.NotificationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["kafka.listener.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
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