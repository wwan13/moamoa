package server.feature.member.query

import server.feature.member.command.domain.Provider

data class MemberSummary(
    val memberId: Long,
    val email: String,
    val provider: Provider,

    val subscribeCount: Long,
    val bookmarkCount: Long
)