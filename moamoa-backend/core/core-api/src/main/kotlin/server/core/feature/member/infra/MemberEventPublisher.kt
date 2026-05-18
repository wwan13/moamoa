package server.core.feature.member.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.member.application.ApplyTemporaryPasswordEvent
import server.core.feature.member.application.MemberUnjoinEvent
import server.core.feature.member.domain.Member
import server.core.feature.member.application.MemberCreateEvent
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

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishApplyTemporaryPassword(member: Member, temporaryPassword: String) {
        eventPublisher.publish(
            ApplyTemporaryPasswordEvent(
                memberId = member.id,
                email = member.email,
                temporaryPassword = temporaryPassword
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishUnjoined(memberId: Long) {
        eventPublisher.publish(
            MemberUnjoinEvent(
                memberId = memberId,
            )
        )
    }
}
