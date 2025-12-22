package server.domain.member

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MemberRepository : CoroutineCrudRepository<Member, Long> {
    suspend fun findByEmail(email: String): Member?

    suspend fun existsByEmail(email: String): Boolean
}