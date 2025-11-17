package de.dkb.api.codeChallenge.notification.repository

import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategory
import de.dkb.api.codeChallenge.notification.model.UserSubscribedCategoryId
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserSubscribedCategoryRepository : JpaRepository<UserSubscribedCategory, UserSubscribedCategoryId> {
    fun findByUserId(userId: UUID): List<UserSubscribedCategory>
    fun existsByUserIdAndCategory(userId: UUID, category: String): Boolean

    /**
     * Find all subscriptions for a user that match the given categories.
     * Used for efficient batch operations.
     */
    @Query("SELECT usc FROM UserSubscribedCategory usc WHERE usc.userId = :userId AND usc.category IN :categories")
    fun findByUserIdAndCategoryIn(
        @Param("userId") userId: UUID,
        @Param("categories") categories: List<String>,
    ): List<UserSubscribedCategory>

    /**
     * Find all subscriptions for a user that are NOT in the given categories.
     * Used for efficient removal of obsolete subscriptions.
     */
    @Query("SELECT usc FROM UserSubscribedCategory usc WHERE usc.userId = :userId AND usc.category NOT IN :categories")
    fun findByUserIdAndCategoryNotIn(
        @Param("userId") userId: UUID,
        @Param("categories") categories: List<String>,
    ): List<UserSubscribedCategory>
}

