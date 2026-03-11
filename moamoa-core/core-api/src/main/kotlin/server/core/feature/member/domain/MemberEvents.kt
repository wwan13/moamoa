package server.core.feature.member.domain

import server.messaging.Event

data class MemberCreateEvent(
    val memberId: Long,
    val email: String,
) : Event
