package server.core.global.security

import server.core.feature.member.domain.MemberRole

data class Passport(
    val memberId: Long,
    val role: MemberRole
)