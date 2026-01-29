package server.feature.submission.application

import org.springframework.stereotype.Service
import server.feature.submission.domain.Submission
import server.feature.submission.domain.SubmissionCreateEvent
import server.feature.submission.domain.SubmissionRepository
import server.infra.db.Transactional
import server.messaging.StreamEventPublisher
import server.messaging.StreamTopic

@Service
class SubmissionService(
    private val transactional: Transactional,
    private val submissionRepository: SubmissionRepository,
    private val defaultTopic: StreamTopic,
    private val eventPublisher: StreamEventPublisher
) {

    suspend fun create(
        command: SubmissionCreateCommand,
        memberId: Long
    ): SubmissionCreateResult = transactional {
        val submission = Submission(
            blogTitle = command.blogTitle,
            blogUrl = command.blogUrl,
            notificationEnabled = command.notificationEnabled,
            accepted = false,
            memberId = memberId,
        )
        val saved = submissionRepository.save(submission)

        val event = SubmissionCreateEvent(
            submissionId = saved.id,
            blogTitle = saved.blogTitle,
            blogUrl = saved.blogUrl,
        )
        eventPublisher.publish(defaultTopic, event)

        SubmissionCreateResult(saved.id)
    }
}
