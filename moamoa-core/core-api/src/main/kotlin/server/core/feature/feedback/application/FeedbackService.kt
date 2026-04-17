package server.core.feature.feedback.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.feedback.domain.Feedback
import server.core.feature.feedback.domain.FeedbackRepository
import server.core.feature.feedback.infra.FeedbackEventPublisher

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val feedbackEventPublisher: FeedbackEventPublisher
) {

    @Transactional
    fun create(
        command: FeedbackCreateCommand
    ): FeedbackCreateResult {
        val feedback = Feedback(
            type = command.type,
            content = command.content,
            email = command.email,
        )
        feedbackRepository.save(feedback)

        feedbackEventPublisher.publishCreated(feedback)

        return FeedbackCreateResult(feedback.id)
    }
}
