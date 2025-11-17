package de.dkb.api.codeChallenge.notification.service

import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.repository.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.repository.UserRepository
import de.dkb.api.codeChallenge.notification.repository.UserSubscribedCategoryRepository
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    data class RegistrationResult(
        val user: User,
        val wasCreated: Boolean,
        val wasUpdated: Boolean,
    )

    @Transactional
    fun registerUser(user: User): RegistrationResult {
        logger.debug("Registering user: userId={}, notificationTypes={}", user.id, user.notifications)
        // Check if user already exists
        val existingUser = userRepository.findById(user.id).orElse(null)
        val notificationsChanged = existingUser?.notifications != user.notifications

        val savedUser = if (existingUser != null) {
            // User exists - update notifications if different (idempotent: same data = no change)
            if (notificationsChanged) {
                existingUser.notifications = user.notifications
                userRepository.save(existingUser)
            } else {
                // Same data - idempotent, return existing user (skip category logic)
                return RegistrationResult(
                    user = existingUser,
                    wasCreated = false,
                    wasUpdated = false,
                )
            }
        } else {
            // New user - save
            userRepository.save(user)
        }

        // Only process category subscriptions if it's a new user or notifications changed
        // Derive categories from notification types and save to user_subscribed_category
        val typeCategories = notificationTypeCategoryRepository.findByNotificationTypeIn(savedUser.notifications.toList())

        // Get distinct categories
        val categories = typeCategories.map { it.category }.distinct()

        // Save category subscriptions (idempotent: only if not exists)
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

        // Remove category subscriptions that are no longer needed (only if notifications changed)
        if (notificationsChanged && existingUser != null) {
            val existingSubscriptions = userSubscribedCategoryRepository.findByUserId(savedUser.id)
            val currentCategories = categories.toSet()
            val subscriptionsToRemove = existingSubscriptions.filter { it.category !in currentCategories }

            subscriptionsToRemove.forEach { subscription ->
                userSubscribedCategoryRepository.delete(subscription)
            }
        }

        val result = RegistrationResult(
            user = savedUser,
            wasCreated = existingUser == null,
            wasUpdated = notificationsChanged && existingUser != null,
        )
        logger.info(
            "User registration completed: userId={}, wasCreated={}, wasUpdated={}, categories={}",
            savedUser.id,
            result.wasCreated,
            result.wasUpdated,
            categories,
        )
        return result
    }

    fun sendNotification(notificationDto: NotificationDto): ResponseEntity<Map<String, String>> {
        logger.debug(
            "Processing notification request: userId={}, type={}",
            notificationDto.userId,
            notificationDto.notificationType,
        )
        // Check if user exists
        val user = userRepository.findById(notificationDto.userId).orElse(null)
            ?: run {
                logger.warn("User not found: userId={}", notificationDto.userId)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "User with id ${notificationDto.userId} not found"))
            }

        val requestedTypeString = notificationDto.notificationType

        // Check if notification type exists
        val typeCategory = notificationTypeCategoryRepository.findById(requestedTypeString).orElse(null)
            ?: run {
                logger.warn("Notification type not found: type={}", requestedTypeString)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Notification type '$requestedTypeString' does not exist"))
            }

        // Check if user is subscribed to the category
        val hasCategorySubscription =
            userSubscribedCategoryRepository.existsByUserIdAndCategory(user.id, typeCategory.category)
        if (!hasCategorySubscription) {
            logger.warn(
                "User not subscribed to category: userId={}, category={}, type={}",
                user.id,
                typeCategory.category,
                requestedTypeString,
            )
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "User ${user.id} is not subscribed to category '${typeCategory.category}' for notification type '$requestedTypeString'"))
        }

        // Send notification
        logger.info(
            "Sending notification: type={}, userId={}, message={}",
            notificationDto.notificationType,
            user.id,
            notificationDto.message,
        )
        return ResponseEntity.ok(mapOf("message" to "Notification sent successfully"))
    }

    @Transactional
    fun addNotificationType(notificationType: String, category: String) {
        logger.info("Adding notification type: type={}, category={}", notificationType, category)
        // Validate category
        try {
            de.dkb.api.codeChallenge.notification.model.NotificationCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid category provided: category={}", category)
            throw IllegalArgumentException("Invalid category: $category. Must be one of: A, B")
        }

        // Check if notification type already exists
        if (notificationTypeCategoryRepository.existsById(notificationType)) {
            logger.warn("Notification type already exists: type={}", notificationType)
            throw IllegalArgumentException("Notification type '$notificationType' already exists")
        }

        // Save a new notification type to category mapping
        notificationTypeCategoryRepository.save(
            de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory(
                notificationType = notificationType,
                category = category,
            ),
        )
        logger.info("Successfully added notification type: type={}, category={}", notificationType, category)
    }
}

