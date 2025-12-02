package de.dkb.api.codeChallenge.notification.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification_type_category")
data class NotificationTypeCategory(
    @Id
    @Column(name = "notification_type", length = 50)
    val notificationType: String,
    @Column(name = "category", length = 10, nullable = false)
    val category: String,
) {
    // Default constructor for Hibernate
    constructor() : this("", "")
}

