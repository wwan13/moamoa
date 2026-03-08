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

        submission.created()
        val result = extractSingleEvent(submission) as SubmissionCreateEvent

        result shouldBe SubmissionCreateEvent(
            submissionId = 1L,
            blogTitle = "Awesome Blog",
            blogUrl = "https://example.com"
        )
    }

    private fun extractSingleEvent(entity: Any): Any {
        var type: Class<*>? = entity.javaClass
        while (type != null) {
            runCatching {
                val field = type.getDeclaredField("domainEvents")
                field.isAccessible = true
                val events = field.get(entity) as MutableCollection<*>
                return events.single()!!
            }
            type = type.superclass
        }
        error("domainEvents field not found")
    }
}
