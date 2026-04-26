package server.admin.feature.member.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminMemberRepository : JpaRepository<AdminMember, Long> {
    fun findByEmail(email: String): AdminMember?
}