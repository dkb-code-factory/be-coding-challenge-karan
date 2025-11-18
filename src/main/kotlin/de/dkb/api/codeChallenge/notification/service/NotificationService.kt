package de.dkb.api.codeChallenge.notification.service

import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.exception.NotificationTypeNotFoundException
import de.dkb.api.codeChallenge.notification.exception.UserNotSubscribedException
import de.dkb.api.codeChallenge.notification.exception.UserNotFoundException
import de.dkb.api.codeChallenge.notification.model.NotificationCategory
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.repository.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.repository.UserRepository
import de.dkb.api.codeChallenge.notification.repository.UserSubscribedCategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
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
            if (notificationsChanged) {
                existingUser.notifications = user.notifications
                userRepository.save(existingUser)
            } else {
                // Same data - idempotent, return existing user
                return RegistrationResult(
                    user = existingUser,
                    wasCreated = false,
                    wasUpdated = false,
                )
            }
        } else {
            userRepository.save(user)
        }

        // Derive categories from notification types and save to user_subscribed_category
        val allTypeCategories = notificationTypeCategoryRepository.findAll()
        val typeCategories = allTypeCategories.filter { it.notificationType in savedUser.notifications }

        // Get distinct categories
        val categories = typeCategories.map { it.category }.distinct()

        // Batch save: Get existing subscriptions, then insert only new ones
        val existingSubscriptions = if (categories.isNotEmpty()) {
            userSubscribedCategoryRepository.findByUserIdAndCategoryIn(savedUser.id, categories)
        } else {
            emptyList()
        }
        val existingCategories = existingSubscriptions.map { it.category }.toSet()
        val categoriesToInsert = categories.filter { it !in existingCategories }
            .map { UserSubscribedCategory(userId = savedUser.id, category = it) }

        // Batch insert new subscriptions
        if (categoriesToInsert.isNotEmpty()) {
            userSubscribedCategoryRepository.saveAll(categoriesToInsert)
        }

        // Remove obsolete subscriptions if notifications changed
        if (notificationsChanged && existingUser != null && categories.isNotEmpty()) {
            val subscriptionsToRemove = userSubscribedCategoryRepository.findByUserIdAndCategoryNotIn(
                savedUser.id,
                categories,
            )
            // Batch delete obsolete subscriptions
            if (subscriptionsToRemove.isNotEmpty()) {
                userSubscribedCategoryRepository.deleteAll(subscriptionsToRemove)
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

    fun sendNotification(notificationDto: NotificationDto) {
        logger.debug(
            "Processing notification request: userId={}, type={}",
            notificationDto.userId,
            notificationDto.notificationType,
        )
        // Check if user exists
        val user = userRepository.findById(notificationDto.userId).orElse(null)
            ?: run {
                logger.warn("User not found: userId={}", notificationDto.userId)
                throw UserNotFoundException(notificationDto.userId)
            }

        val requestedTypeString = notificationDto.notificationType

        // Check if notification type exists
        val typeCategory = notificationTypeCategoryRepository.findById(requestedTypeString).orElse(null)
            ?: run {
                logger.warn("Notification type not found: type={}", requestedTypeString)
                throw NotificationTypeNotFoundException(requestedTypeString)
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
            throw UserNotSubscribedException(user.id, typeCategory.category, requestedTypeString)
        }

        // Send notification
        logger.info(
            "Sending notification: type={}, userId={}, message={}",
            notificationDto.notificationType,
            user.id,
            notificationDto.message,
        )
    }

    @Transactional
    @CacheEvict(value = ["notificationTypeCategories"], allEntries = true)
    fun addNotificationType(notificationType: String, category: String) {
        logger.info("Adding notification type: type={}, category={}", notificationType, category)
        
        // Validate category
        val validCategories = NotificationCategory.entries.map { it.name }
        if (category !in validCategories) {
            logger.error("Invalid category provided: category={}", category)
            throw IllegalArgumentException("Invalid category: $category. Must be one of: ${validCategories.joinToString(", ")}")
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
        // Cache is evicted by @CacheEvict annotation
        logger.info("Successfully added notification type: type={}, category={}. Cache invalidated.", notificationType, category)
    }
}

