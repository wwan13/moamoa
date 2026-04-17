package server.core.feature.feedback.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.feedback.application.FeedbackCreateEvent
import server.core.feature.feedback.domain.Feedback
import server.core.infra.outbox.TransactionalEventPublisher

@Component
class FeedbackEventPublisher(
    private val transactionalEventPublisher: TransactionalEventPublisher
) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun publishCreated(feedback: Feedback) {
        transactionalEventPublisher.publish(
            FeedbackCreateEvent(
                feedbackId = feedback.id,
                feedbackType = feedback.type,
                content = feedback.content,
                email = feedback.email,
            )
        )
    }
}
