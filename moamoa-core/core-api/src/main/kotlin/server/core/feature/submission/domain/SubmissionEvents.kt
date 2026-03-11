package server.core.feature.submission.domain

import server.messaging.Event

data class SubmissionCreateEvent(
    val submissionId: Long,
    val blogTitle: String,
    val blogUrl: String,
) : Event
