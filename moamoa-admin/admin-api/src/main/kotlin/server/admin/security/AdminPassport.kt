package server.admin.security

import server.admin.feature.member.domain.AdminMemberRole

internal data class AdminPassport(
    val memberId: Long,
    val role: AdminMemberRole
)