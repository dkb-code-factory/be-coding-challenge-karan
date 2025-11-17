package de.dkb.api.codeChallenge.notification.repository

import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategoryId
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserSubscribedCategoryRepository : JpaRepository<UserSubscribedCategory, UserSubscribedCategoryId> {
    fun findByUserId(userId: UUID): List<UserSubscribedCategory>
    fun existsByUserIdAndCategory(userId: UUID, category: String): Boolean
}

