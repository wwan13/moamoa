package server.core.feature.submission.domain

import server.core.support.domain.DomainEvent

data class SubmissionCreateEvent(
    val submissionId: Long,
    val blogTitle: String,
    val blogUrl: String,
) : DomainEvent
