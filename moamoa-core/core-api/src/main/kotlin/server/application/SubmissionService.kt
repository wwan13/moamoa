package server.application

import org.springframework.stereotype.Service
import server.domain.submission.Submission
import server.domain.submission.SubmissionRepository
import server.infra.db.Transactional

@Service
class SubmissionService(
    private val transactional: Transactional,
    private val submissionRepository: SubmissionRepository
) {

    suspend fun create(
        command: SubmissionCreateCommand,
        memberId: Long
    ): SubmissionCreateResult = transactional {
        val submission = Submission(
            blogTitle = command.blogTitle,
            blogUrl = command.blogUrl,
            accepted = false,
            memberId = memberId,
        )
        val saved = submissionRepository.save(submission)
        SubmissionCreateResult(saved.id)
    }
}
