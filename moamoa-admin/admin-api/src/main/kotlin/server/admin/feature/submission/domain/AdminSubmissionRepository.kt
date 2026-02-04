package server.admin.feature.submission.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminSubmissionRepository : CoroutineCrudRepository<AdminSubmission, Long> {
    suspend fun deleteAllByMemberId(memberId: Long): Long
}