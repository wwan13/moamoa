package server.core.feature.member.query

import server.core.feature.member.domain.MemberProvider

data class MemberSummary(
    val memberId: Long,
    val email: String,
    val provider: MemberProvider,

    val subscribeCount: Long,
    val bookmarkCount: Long
)
