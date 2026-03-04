package server.admin.feature.submission.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminSubmissionRepository : JpaRepository<AdminSubmission, Long> {
    fun deleteAllByMemberId(memberId: Long): Long
}