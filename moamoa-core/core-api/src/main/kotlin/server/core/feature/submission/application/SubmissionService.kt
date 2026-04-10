package server.core.feature.submission.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.submission.infra.SubmissionEventPublisher
import server.global.logging.biz

@Service
class SubmissionService(
    private val submissionRepository: SubmissionRepository,
    private val submissionEventPublisher: SubmissionEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
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

        submissionEventPublisher.publishCreated(saved)
        logger.biz.info { "제보를 생성합니다" }

        return SubmissionCreateResult(saved.id)
    }
}
