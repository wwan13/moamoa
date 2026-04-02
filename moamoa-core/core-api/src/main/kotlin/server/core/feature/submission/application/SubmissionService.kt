package server.core.feature.submission.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionCreateEvent
import server.core.feature.submission.domain.SubmissionRepository
import server.core.infra.event.TransactionalEventPublisher
import server.global.logging.biz

@Service
@Transactional
class SubmissionService(
    private val submissionRepository: SubmissionRepository,
    private val eventPublisher: TransactionalEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun create(
        command: SubmissionCreateCommand,
        memberId: Long
    ): SubmissionCreateResult {
        val submission = Submission(
            blogTitle = command.blogTitle,
            blogUrl = command.blogUrl,
            notificationEnabled = command.notificationEnabled,
            accepted = false,
            memberId = memberId,
        )
        val saved = submissionRepository.save(submission)

        eventPublisher.publish(
            SubmissionCreateEvent(
                submissionId = saved.id,
                blogTitle = saved.blogTitle,
                blogUrl = saved.blogUrl,
            )
        )
        logger.biz.info { "제보를 생성합니다" }

        return SubmissionCreateResult(saved.id)
    }
}
