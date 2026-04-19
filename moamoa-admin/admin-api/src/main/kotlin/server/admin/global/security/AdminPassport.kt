package server.admin.global.security

import server.core.feature.member.domain.MemberRole

internal data class AdminPassport(
    val memberId: Long,
    val role: MemberRole
)
