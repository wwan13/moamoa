package server.core.feature.member.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.member.domain.Member
import server.core.feature.member.domain.MemberCreateEvent
import server.core.infra.outbox.TransactionalEventPublisher

@Component
class MemberEventPublisher(
    private val eventPublisher: TransactionalEventPublisher,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun publishCreated(member: Member) {
        eventPublisher.publish(
            MemberCreateEvent(
                memberId = member.id,
                email = member.email,
            )
        )
    }
}
