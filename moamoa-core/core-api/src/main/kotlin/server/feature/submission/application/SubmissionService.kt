package server.feature.submission.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.global.logging.event
import server.feature.submission.domain.Submission
import server.feature.submission.domain.SubmissionRepository
import server.infra.db.transaction.Transactional

@Service
class SubmissionService(
    private val transactional: Transactional,
    private val submissionRepository: SubmissionRepository
) {
    private val logger = KotlinLogging.logger {}

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

        val event = saved.created()
        registerEvent(event)
        logger.event.info(event) {
            "제보 생성 이벤트를 발행했습니다"
        }

        SubmissionCreateResult(saved.id)
    }
}
