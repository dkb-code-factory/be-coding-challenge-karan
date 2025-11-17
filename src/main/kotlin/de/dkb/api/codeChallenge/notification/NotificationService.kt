package de.dkb.api.codeChallenge.notification

import de.dkb.api.codeChallenge.notification.model.NotificationDto
import de.dkb.api.codeChallenge.notification.model.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserRepository
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val notificationTypeCategoryRepository: NotificationTypeCategoryRepository,
    private val userSubscribedCategoryRepository: UserSubscribedCategoryRepository,
) {

    @Transactional
    fun registerUser(user: User): User {
        // Save user with notification types (legacy)
        val savedUser = userRepository.save(user)

        // Derive categories from notification types and save to user_subscribed_category
        val typeCategories = notificationTypeCategoryRepository.findByNotificationTypeIn(user.notifications.toList())

        // Get distinct categories
        val categories = typeCategories.map { it.category }.distinct()

        // Save category subscriptions
        categories.forEach { category ->
            if (!userSubscribedCategoryRepository.existsByUserIdAndCategory(savedUser.id, category)) {
                userSubscribedCategoryRepository.save(
                    UserSubscribedCategory(
                        userId = savedUser.id,
                        category = category,
                    ),
                )
            }
        }

        return savedUser
    }

    fun sendNotification(notificationDto: NotificationDto): ResponseEntity<Map<String, String>> {
        // Check if user exists
        val user = userRepository.findById(notificationDto.userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User with id ${notificationDto.userId} not found"))

        val requestedTypeString = notificationDto.notificationType

        // Check if notification type exists
        val typeCategory = notificationTypeCategoryRepository.findById(requestedTypeString).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Notification type '$requestedTypeString' does not exist"))

        // Check if user is subscribed to the category
        val hasCategorySubscription =
            userSubscribedCategoryRepository.existsByUserIdAndCategory(user.id, typeCategory.category)
        if (!hasCategorySubscription) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "User ${user.id} is not subscribed to category '${typeCategory.category}' for notification type '$requestedTypeString'"))
        }

        // Send notification
        println(
            "Sending notification of type ${notificationDto.notificationType}" +
                    " to user ${user.id}: ${notificationDto.message}",
        )
        return ResponseEntity.ok(mapOf("message" to "Notification sent successfully"))
    }

    @Transactional
    fun addNotificationType(notificationType: String, category: String) {
        // Validate category
        try {
            de.dkb.api.codeChallenge.notification.model.NotificationCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid category: $category. Must be one of: A, B")
        }

        // Check if notification type already exists
        if (notificationTypeCategoryRepository.existsById(notificationType)) {
            throw IllegalArgumentException("Notification type '$notificationType' already exists")
        }

        // Save a new notification type to category mapping
        notificationTypeCategoryRepository.save(
            de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory(
                notificationType = notificationType,
                category = category,
            ),
        )
    }
}