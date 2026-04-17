package server.core.feature.submission.application

import server.messaging.Event

data class SubmissionCreateEvent(
    val submissionId: Long,
    val blogTitle: String,
    val blogUrl: String,
) : Event
