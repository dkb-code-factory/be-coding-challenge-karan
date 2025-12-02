package de.dkb.api.codeChallenge.notification.exception

class NotificationTypeNotFoundException(notificationType: String) : RuntimeException("Notification type '$notificationType' does not exist")

