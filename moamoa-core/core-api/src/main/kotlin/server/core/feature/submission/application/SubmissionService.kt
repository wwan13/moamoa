package server.core.feature.submission.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionRepository
import server.core.infra.db.transaction.Transactional
import server.global.logging.event

@Service
class SubmissionService(
    private val transactional: Transactional,
    private val submissionRepository: SubmissionRepository
) {
    private val logger = KotlinLogging.logger {}

    fun create(
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

        val event = saved.created()
        registerEvent(event)
        logger.event.info(event) {
            "제보 생성 이벤트를 발행했습니다"
        }

        SubmissionCreateResult(saved.id)
    }
}
