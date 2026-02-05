package server.admin.feature.member.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminMemberRepository : CoroutineCrudRepository<AdminMember, Long> {
    suspend fun findByEmail(email: String): AdminMember?
}