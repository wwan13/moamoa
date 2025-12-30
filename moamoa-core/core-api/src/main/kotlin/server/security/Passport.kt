package server.security

import server.domain.member.MemberRole

data class Passport(
    val memberId: Long,
    val role: MemberRole
)