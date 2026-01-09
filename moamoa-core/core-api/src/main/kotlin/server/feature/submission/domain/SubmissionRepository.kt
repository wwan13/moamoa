package server.feature.submission.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SubmissionRepository : CoroutineCrudRepository<Submission, Long> {
}