package de.dkb.api.codeChallenge.notification.model

import java.util.UUID

data class NotificationDto(
    val userId: UUID,
    val notificationType: String,
    val message: String,
)