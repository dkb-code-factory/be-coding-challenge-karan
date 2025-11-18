package de.dkb.api.codeChallenge.notification.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.dkb.api.codeChallenge.notification.dto.AddNotificationTypeDto
import de.dkb.api.codeChallenge.notification.dto.NotificationDto
import de.dkb.api.codeChallenge.notification.model.NotificationTypeCategory
import de.dkb.api.codeChallenge.notification.model.User
import de.dkb.api.codeChallenge.notification.repository.NotificationTypeCategoryRepository
import de.dkb.api.codeChallenge.notification.repository.UserRepository
import de.dkb.api.codeChallenge.notification.repository.UserSubscribedCategoryRepository
import de.dkb.api.codeChallenge.notification.service.NotificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    ]
)
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.liquibase.enabled=false"
    ]
)
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var notificationTypeCategoryRepository: NotificationTypeCategoryRepository
    @Autowired private lateinit var userSubscribedCategoryRepository: UserSubscribedCategoryRepository

    @Autowired private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        userSubscribedCategoryRepository.deleteAll()
        userRepository.deleteAll()
        notificationTypeCategoryRepository.deleteAll()

        notificationTypeCategoryRepository.save(NotificationTypeCategory("type1", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type2", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type3", "A"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type4", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type5", "B"))
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type6", "A"))
    }

    // ----------------------------------------------------------
    // REGISTER TESTS
    // ----------------------------------------------------------

    @Test
    fun `POST register should create new user and return 201`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1", "type2"))

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("User registered successfully"))
            .andExpect(jsonPath("$.user.id").value(userId.toString()))
    }

    @Test
    fun `POST register should return 200 when user already exists with same data`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        userRepository.save(user)

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User already exists with the same data"))
    }

    @Test
    fun `POST register should return 200 when updating existing user`() {
        val userId = UUID.randomUUID()
        val initialUser = User(id = userId, notifications = mutableSetOf("type1"))
        userRepository.save(initialUser)

        val updatedUser = User(id = userId, notifications = mutableSetOf("type4"))

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("User updated successfully"))
    }

    // ----------------------------------------------------------
    // NOTIFICATION TESTS
    // ----------------------------------------------------------

    @Test
    fun `POST notify should send notification when user has category subscription`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1"))
        notificationService.registerUser(user)

        val dto = NotificationDto(userId, "type6", "Test message")

        mockMvc.perform(
            post("/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Notification sent successfully"))
    }

    @Test
    fun `POST notify should return 404 when user does not exist`() {
        val dto = NotificationDto(UUID.randomUUID(), "type1", "Test")

        mockMvc.perform(
            post("/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `POST notify should return 403 when user is not subscribed to category`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, notifications = mutableSetOf("type1")) // Category A
        notificationService.registerUser(user)

        val dto = NotificationDto(userId, "type4", "Test") // Category B

        mockMvc.perform(
            post("/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").exists())
    }

    // ----------------------------------------------------------
    // ADMIN TESTS
    // ----------------------------------------------------------

    @Test
    fun `POST admin notification-types should add new notification type`() {
        val request = AddNotificationTypeDto("type7", "B")

        mockMvc.perform(
            post("/admin/notification-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Notification type 'type7' added to category 'B'"))

        val saved = notificationTypeCategoryRepository.findById("type7")
        assertThat(saved).isPresent
    }

    @Test
    fun `POST admin notification-types should return 400 when type already exists`() {
        notificationTypeCategoryRepository.save(NotificationTypeCategory("type7", "B"))
        val request = AddNotificationTypeDto("type7", "B")

        mockMvc.perform(
            post("/admin/notification-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `POST admin notification-types should return 400 when category is invalid`() {
        val request = AddNotificationTypeDto("type7", "C")

        mockMvc.perform(
            post("/admin/notification-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }
}
