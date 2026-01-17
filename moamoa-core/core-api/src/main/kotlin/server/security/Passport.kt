package server.security

import server.feature.member.command.domain.MemberRole

data class Passport(
    val memberId: Long,
    val role: MemberRole
)