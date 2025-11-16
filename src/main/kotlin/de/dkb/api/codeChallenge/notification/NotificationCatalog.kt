package de.dkb.api.codeChallenge.notification

import de.dkb.api.codeChallenge.notification.model.NotificationCategory
import de.dkb.api.codeChallenge.notification.model.NotificationType
import org.springframework.stereotype.Component

@Component
class NotificationCatalog {

    private val categoryToTypes: Map<NotificationCategory, Set<NotificationType>> =
        mapOf(
            NotificationCategory.A to setOf(
                NotificationType.type1,
                NotificationType.type2,
                NotificationType.type3,
            ),
            NotificationCategory.B to setOf(
                NotificationType.type4,
                NotificationType.type5,
            ),
        )

    private val typeToCategory: Map<NotificationType, NotificationCategory> =
        categoryToTypes
            .flatMap { (category, types) -> types.map { it to category } }
            .toMap()

    fun getCategoryFor(type: NotificationType): NotificationCategory? =
        typeToCategory[type]

    fun getTypesFor(category: NotificationCategory): Set<NotificationType> =
        categoryToTypes[category].orEmpty()
}


