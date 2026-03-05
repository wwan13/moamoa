package server.core.feature.submission.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionCreateEvent
import test.UnitTest

class SubmissionTest : UnitTest() {
    @Test
    fun `제보 생성 시 SubmissionCreateEvent를 반환한다`() {
        val submission = Submission(
            id = 1L,
            blogTitle = "Awesome Blog",
            blogUrl = "https://example.com",
            notificationEnabled = true,
            accepted = false,
            memberId = 10L
        )

        val result = submission.created()

        result shouldBe SubmissionCreateEvent(
            submissionId = 1L,
            blogTitle = "Awesome Blog",
            blogUrl = "https://example.com"
        )
    }
}
