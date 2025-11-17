package de.dkb.api.codeChallenge.notification.dto

data class AddNotificationTypeRequest(
    val notificationType: String,
    val category: String,
)

