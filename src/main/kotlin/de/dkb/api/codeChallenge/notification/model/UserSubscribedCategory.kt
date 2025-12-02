package de.dkb.api.codeChallenge.notification.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_subscribed_category")
@IdClass(UserSubscribedCategoryId::class)
data class UserSubscribedCategory(
    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    val userId: UUID,
    @Id
    @Column(name = "category", length = 10)
    val category: String,
) {
    // Default constructor for Hibernate
    constructor() : this(UUID.randomUUID(), "")
}

data class UserSubscribedCategoryId(
    val userId: UUID = UUID.randomUUID(),
    val category: String = "",
)

