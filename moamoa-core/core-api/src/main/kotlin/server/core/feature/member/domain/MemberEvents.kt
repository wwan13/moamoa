package server.core.feature.member.domain

import server.core.support.domain.DomainEvent

data class MemberCreateEvent(
    val memberId: Long,
    val email: String,
) : DomainEvent
