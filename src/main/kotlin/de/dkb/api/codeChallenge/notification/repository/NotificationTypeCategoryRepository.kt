package de.dkb.api.codeChallenge.notification.repository

import de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationTypeCategoryRepository : JpaRepository<NotificationTypeCategory, String> {
    fun findByNotificationTypeIn(notificationTypes: List<String>): List<NotificationTypeCategory>
    fun findByCategory(category: String): List<NotificationTypeCategory>

    /**
     * Find all notification type categories. Cached for performance.
     * Cache is invalidated when new types are added via addNotificationType.
     */
    @Cacheable(value = ["notificationTypeCategories"], key = "'all'")
    override fun findAll(): List<NotificationTypeCategory>

    /**
     * Find by ID. Cached for performance.
     */
    @Cacheable(value = ["notificationTypeCategories"], key = "#p0")
    override fun findById(id: String): java.util.Optional<NotificationTypeCategory>
}

