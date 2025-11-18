package de.dkb.api.codeChallenge.notification.exception

import java.util.UUID

class UserNotFoundException(userId: UUID) : RuntimeException("User with id $userId not found")

