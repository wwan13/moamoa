package server.core.feature.submission.application

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionRepository
import server.core.feature.submission.infra.SubmissionEventPublisher
import server.core.fixture.createSubmission
import test.UnitTest

class SubmissionServiceTest : UnitTest() {
    @Test
    fun `제출 생성 시 저장한다`() = runTest {
        val submissionRepository = mockk<SubmissionRepository>()
        val submissionEventPublisher = mockk<SubmissionEventPublisher>(relaxed = true)
        val service = SubmissionService(submissionRepository, submissionEventPublisher)

        val command = SubmissionCreateCommand(
            blogTitle = "moamoa blog",
            blogUrl = "https://blog.example.com",
            notificationEnabled = true,
        )
        val memberId = 42L
        val savedId = 1L
        val submissionSlot = slot<Submission>()

        coEvery { submissionRepository.save(capture(submissionSlot)) } returns createSubmission(
            id = savedId,
            blogTitle = command.blogTitle,
            blogUrl = command.blogUrl,
            notificationEnabled = command.notificationEnabled,
            memberId = memberId
        )

        val result = service.create(command, memberId)

        result.submissionId shouldBe savedId
        submissionSlot.captured.blogTitle shouldBe command.blogTitle
        submissionSlot.captured.blogUrl shouldBe command.blogUrl
        submissionSlot.captured.notificationEnabled shouldBe command.notificationEnabled
        submissionSlot.captured.accepted shouldBe false
        submissionSlot.captured.memberId shouldBe memberId
    }

    @Test
    fun `제출 생성 시 SubmissionCreateEvent 가 발행된다`() = runTest {
        val submissionRepository = mockk<SubmissionRepository>()
        val submissionEventPublisher = mockk<SubmissionEventPublisher>(relaxed = true)
        val service = SubmissionService(submissionRepository, submissionEventPublisher)

        val command = SubmissionCreateCommand(
            blogTitle = "moamoa blog",
            blogUrl = "https://blog.example.com",
            notificationEnabled = true,
        )
        val memberId = 42L
        val savedId = 1L

        coEvery { submissionRepository.save(any()) } returns createSubmission(
            id = savedId,
            blogTitle = command.blogTitle,
            blogUrl = command.blogUrl,
            notificationEnabled = command.notificationEnabled,
            memberId = memberId
        )

        service.create(command, memberId)

        verify(exactly = 1) {
            submissionEventPublisher.publishCreated(
                match<Submission> {
                    it.id == savedId &&
                        it.blogTitle == command.blogTitle &&
                        it.blogUrl == command.blogUrl
                }
            )
        }
    }
}
