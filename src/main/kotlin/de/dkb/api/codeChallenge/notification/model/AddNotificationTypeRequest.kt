package de.dkb.api.codeChallenge.notification.model

data class AddNotificationTypeRequest(
    val notificationType: String,
    val category: String,
)

