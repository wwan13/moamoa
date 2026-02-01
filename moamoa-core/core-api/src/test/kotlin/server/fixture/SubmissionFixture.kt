package server.fixture

import server.feature.submission.domain.Submission

fun createSubmission(
    id: Long = 0L,
    blogTitle: String = "기본 제목",
    blogUrl: String = "https://example.com",
    notificationEnabled: Boolean = true,
    accepted: Boolean = false,
    memberId: Long = 1L
): Submission = Submission(
    id = id,
    blogTitle = blogTitle,
    blogUrl = blogUrl,
    notificationEnabled = notificationEnabled,
    accepted = accepted,
    memberId = memberId
)
