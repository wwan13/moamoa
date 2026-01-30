package server.feature.member.command.domain

data class MemberCreateEvent(
    val memberId: Long,
    val email: String,
)