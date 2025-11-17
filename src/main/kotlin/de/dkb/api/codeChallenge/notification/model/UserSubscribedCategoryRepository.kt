package de.dkb.api.codeChallenge.notification.model

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSubscribedCategoryRepository : JpaRepository<UserSubscribedCategory, UserSubscribedCategoryId> {
    fun findByUserId(userId: UUID): List<UserSubscribedCategory>
    fun existsByUserIdAndCategory(userId: UUID, category: String): Boolean
}

