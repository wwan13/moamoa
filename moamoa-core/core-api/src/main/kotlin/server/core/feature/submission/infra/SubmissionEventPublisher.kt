package server.core.feature.submission.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionCreateEvent
import server.core.infra.event.TransactionalEventPublisher

@Component
class SubmissionEventPublisher(
    private val eventPublisher: TransactionalEventPublisher,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun publishCreated(submission: Submission) {
        eventPublisher.publish(
            SubmissionCreateEvent(
                submissionId = submission.id,
                blogTitle = submission.blogTitle,
                blogUrl = submission.blogUrl,
            )
        )
    }
}
