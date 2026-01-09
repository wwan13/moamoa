package server.security

import server.feature.member.domain.MemberRole

data class Passport(
    val memberId: Long,
    val role: MemberRole
)