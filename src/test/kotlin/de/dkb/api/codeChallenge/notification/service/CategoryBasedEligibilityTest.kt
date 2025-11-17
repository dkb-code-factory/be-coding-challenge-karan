package de.dkb.api.codeChallenge.notification.service

import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.repository.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.repository.UserRepository
import de.dkb.api.codeChallenge.notification.repository.UserSubscribedCategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import java.util.UUID

/**
 * Tests for the main requirement: Existing users should receive notifications
 * for new types within their subscribed categories without re-registration.
 */
@DataJpaTest
@Import(NotificationService::class)
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false"
])
class CategoryBasedEligibilityTest {

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationTypeCategoryRepository: NotificationTypeCategoryRepository

    @Autowired
    private lateinit var userSubscribedCategoryRepository: UserSubscribedCategoryRepository

    @BeforeEach
    fun setUp() {
        // Clean up before each test
        userSubscribedCategoryRepository.deleteAll()
        userRepository.deleteAll()
        notificationTypeCategoryRepository.deleteAll()

        // Setup initial notification type categories (type1-6)
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type1", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type2", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type3", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type4", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type5", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type6", "A"))
    }

    @Test
    fun `existing user with type1 should receive type6 notification both Category A`() {
        // Given - user registered with type1 (Category A)
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        // Verify user has Category A subscription
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].category).isEqualTo("A")

        // When - send notification with type6 (also Category A, but user doesn't have it in notifications)
        val notificationDto = NotificationDto(userId = userId, notificationType = "type6", message = "Category A notification")

        val response = notificationService.sendNotification(notificationDto)

        // Then - should receive notification because both are in Category A
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `existing user with type4 should receive type7 notification after type7 is added to Category B`() {
        // Given - user registered with type4 (Category B)
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type4"))
        notificationService.registerUser(user)

        // Verify user has Category B subscription
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].category).isEqualTo("B")

        // Add new type7 to Category B (simulating admin adding new type)
        notificationService.addNotificationType("type7", "B")

        // When - send notification with type7 (user doesn't have it in their notifications)
        val notificationDto = NotificationDto(userId = userId, notificationType = "type7", message = "New Category B type")

        val response = notificationService.sendNotification(notificationDto)

        // Then - should receive notification because user has Category B subscription
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `user with type1 should NOT receive type4 notification different categories`() {
        // Given - user registered with type1 (Category A)
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        // When - send notification with type4 (Category B)
        val notificationDto = NotificationDto(userId = userId, notificationType = "type4", message = "Category B notification")

        val response = notificationService.sendNotification(notificationDto)

        // Then - should NOT receive notification (different category)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `user with multiple categories should receive notifications from both categories`() {
        // Given - user registered with type1 (Category A) and type4 (Category B)
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1", "type4"))
        notificationService.registerUser(user)

        // Verify user has both category subscriptions
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(2)
        assertThat(subscriptions.map { it.category }).containsExactlyInAnyOrder("A", "B")

        // When - send notification with type6 (Category A)
        val notificationDtoA = NotificationDto(userId = userId, notificationType = "type6", message = "Category A")
        val responseA = notificationService.sendNotification(notificationDtoA)

        // Then - should receive Category A notification
        assertThat(responseA.statusCode).isEqualTo(HttpStatus.OK)

        // When - send notification with type5 (Category B)
        val notificationDtoB = NotificationDto(userId = userId, notificationType = "type5", message = "Category B")
        val responseB = notificationService.sendNotification(notificationDtoB)

        // Then - should receive Category B notification
        assertThat(responseB.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `user should receive all types in their subscribed category even if not explicitly registered`() {
        // Given - user registered with only type1 (Category A)
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        // User should receive type1, type2, type3, and type6 (all Category A)
        val categoryATypes = listOf("type1", "type2", "type3", "type6")

        categoryATypes.forEach { type ->
            val notificationDto = NotificationDto(userId = userId, notificationType = type, message = "Category A: $type")
            val response = notificationService.sendNotification(notificationDto)
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
                .withFailMessage("User should receive notification for $type (Category A)")
        }
    }
}

