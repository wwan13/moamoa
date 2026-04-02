package server.core.feature.submission.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest

class SubmissionTest : UnitTest() {
    @Test
    fun `제보 생성 시 입력값을 보관한다`() {
        val submission = Submission(
            id = 1L,
            blogTitle = "Awesome Blog",
            blogUrl = "https://example.com",
            notificationEnabled = true,
            accepted = false,
            memberId = 10L
        )

        submission.id shouldBe 1L
        submission.blogTitle shouldBe "Awesome Blog"
        submission.blogUrl shouldBe "https://example.com"
        submission.notificationEnabled shouldBe true
        submission.accepted shouldBe false
        submission.memberId shouldBe 10L
    }
}
