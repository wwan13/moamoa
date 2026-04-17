package server.core.feature.member.application

import server.messaging.Event

data class MemberCreateEvent(
    val memberId: Long,
    val email: String,
) : Event

data class ApplyTemporaryPasswordEvent(
    val memberId: Long,
    val email: String,
    val temporaryPassword: String
) : Event
