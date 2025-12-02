package de.dkb.api.codeChallenge.notification.service

import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.repository.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.repository.UserRepository
import de.dkb.api.codeChallenge.notification.repository.UserSubscribedCategoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.junit.jupiter.api.assertThrows
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@DataJpaTest
@Import(NotificationService::class)
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false"
])
class NotificationServiceTest {

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

        // Setup initial notification type categories
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type1", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type2", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type3", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type4", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type5", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type6", "A"))
    }

    @Test
    fun `should register new user and create category subscriptions`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1", "type2"))

        // When
        val result = notificationService.registerUser(user)

        // Then
        assertThat(result.wasCreated).isTrue
        assertThat(result.wasUpdated).isFalse
        assertThat(result.user.id).isEqualTo(userId)
        assertThat(result.user.notifications).containsExactlyInAnyOrder("type1", "type2")

        // Verify category subscriptions
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].category).isEqualTo("A")
    }

    @Test
    fun `should register user with multiple categories`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1", "type4"))

        // When
        val result = notificationService.registerUser(user)

        // Then
        assertThat(result.wasCreated).isTrue
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(2)
        assertThat(subscriptions.map { it.category }).containsExactlyInAnyOrder("A", "B")
    }

    @Test
    fun `should return existing user when registering with same data (idempotent)`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        // When
        val result = notificationService.registerUser(user)

        // Then
        assertThat(result.wasCreated).isFalse
        assertThat(result.wasUpdated).isFalse
        assertThat(result.user.id).isEqualTo(userId)
        assertThat(result.user.notifications).containsExactly("type1")
    }

    @Test
    fun `should update user when registering with different notifications`() {
        // Given
        val userId = UUID.randomUUID()
        val initialUser = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(initialUser)

        val updatedUser = User(id = userId, notifications = mutableSetOf("type4"))

        // When
        val result = notificationService.registerUser(updatedUser)

        // Then
        assertThat(result.wasCreated).isFalse
        assertThat(result.wasUpdated).isTrue
        assertThat(result.user.notifications).containsExactly("type4")

        // Verify category subscriptions updated
        val subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].category).isEqualTo("B")
    }

    @Test
    fun `should remove obsolete category subscriptions when notifications change`() {
        // Given
        val userId = UUID.randomUUID()
        val initialUser = User(id = userId, notifications = mutableSetOf("type1", "type4"))
        notificationService.registerUser(initialUser)

        // Verify both categories exist
        var subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(2)

        // When - change to only Category A
        val updatedUser = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(updatedUser)

        // Then
        subscriptions = userSubscribedCategoryRepository.findByUserId(userId)
        assertThat(subscriptions).hasSize(1)
        assertThat(subscriptions[0].category).isEqualTo("A")
    }

    @Test
    fun `should send notification when user has exact type match`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        val notificationDto = NotificationDto(userId = userId, notificationType = "type1", message = "Test message")

        // When/Then - should not throw exception
        notificationService.sendNotification(notificationDto)
    }

    @Test
    fun `should send notification when user has category match (new type)`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1")) // Category A
        notificationService.registerUser(user)

        // Add new type6 to Category A
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type6", "A"))

        val notificationDto = NotificationDto(userId = userId, notificationType = "type6", message = "Test message")

        // When/Then - should not throw exception
        notificationService.sendNotification(notificationDto)
    }

    @Test
    fun `should not send notification when user is not subscribed to category`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1")) // Category A only
        notificationService.registerUser(user)

        val notificationDto = NotificationDto(userId = userId, notificationType = "type4", message = "Test message") // Category B

        // When/Then
        assertThrows<de.dkb.api.codeChallenge.notification.exception.UserNotSubscribedException> {
            notificationService.sendNotification(notificationDto)
        }
    }

    @Test
    fun `should throw exception when user does not exist`() {
        // Given
        val nonExistentUserId = UUID.randomUUID()
        val notificationDto = NotificationDto(userId = nonExistentUserId, notificationType = "type1", message = "Test")

        // When/Then
        assertThrows<de.dkb.api.codeChallenge.notification.exception.UserNotFoundException> {
            notificationService.sendNotification(notificationDto)
        }
    }

    @Test
    fun `should throw exception when notification type does not exist`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        val notificationDto = NotificationDto(userId = userId, notificationType = "nonexistent", message = "Test")

        // When/Then
        assertThrows<de.dkb.api.codeChallenge.notification.exception.NotificationTypeNotFoundException> {
            notificationService.sendNotification(notificationDto)
        }
    }

    @Test
    fun `should add new notification type via admin endpoint`() {
        // When
        notificationService.addNotificationType("type7", "B")

        // Then
        val typeCategory = notificationTypeCategoryRepository.findById("type7")
        assertThat(typeCategory).isPresent
        assertThat(typeCategory.get().category).isEqualTo("B")
    }

    @Test
    fun `should throw exception when adding duplicate notification type`() {
        // Given
        notificationService.addNotificationType("type7", "B")

        // When/Then
        assertThrows<IllegalArgumentException> {
            notificationService.addNotificationType("type7", "B")
        }
    }

    @Test
    fun `should throw exception when adding notification type with invalid category`() {
        // When/Then
        assertThrows<IllegalArgumentException> {
            notificationService.addNotificationType("type7", "C")
        }
    }

    @Test
    fun `should send notification to user with new type added to their category`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type4")) // Category B
        notificationService.registerUser(user)

        // Add new type7 to Category B
        notificationService.addNotificationType("type7", "B")

        val notificationDto = NotificationDto(userId = userId, notificationType = "type7", message = "New type!")

        // When/Then - should not throw exception
        notificationService.sendNotification(notificationDto)
    }
}

