package server.core.feature.submission.domain

import org.springframework.data.jpa.repository.JpaRepository

interface SubmissionRepository : JpaRepository<Submission, Long> {
    fun deleteAllByMemberId(memberId: Long): Long
}