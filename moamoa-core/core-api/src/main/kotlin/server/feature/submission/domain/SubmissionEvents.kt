package server.feature.submission.domain

data class SubmissionCreateEvent(
    val submissionId: Long,
    val blogTitle: String,
    val blogUrl: String,
)