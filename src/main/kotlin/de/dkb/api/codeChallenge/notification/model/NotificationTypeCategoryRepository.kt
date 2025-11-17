package de.dkb.api.codeChallenge.notification.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationTypeCategoryRepository : JpaRepository<NotificationTypeCategory, String> {
    fun findByNotificationTypeIn(notificationTypes: List<String>): List<NotificationTypeCategory>
    fun findByCategory(category: String): List<NotificationTypeCategory>
}

