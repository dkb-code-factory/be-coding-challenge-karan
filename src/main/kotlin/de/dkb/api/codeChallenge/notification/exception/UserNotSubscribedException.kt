package de.dkb.api.codeChallenge.notification.exception

import java.util.UUID

class UserNotSubscribedException(userId: UUID, category: String, notificationType: String) :
    RuntimeException("User $userId is not subscribed to category '$category' for notification type '$notificationType'")

