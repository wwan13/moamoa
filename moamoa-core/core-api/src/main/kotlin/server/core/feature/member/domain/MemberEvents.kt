package server.core.feature.member.domain

data class MemberCreateEvent(
    val memberId: Long,
    val email: String,
)