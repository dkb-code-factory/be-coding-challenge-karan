package de.dkb.api.codeChallenge.notification.repository

import de.dkb.api.codeChallenge.notification.model.User
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, UUID>

