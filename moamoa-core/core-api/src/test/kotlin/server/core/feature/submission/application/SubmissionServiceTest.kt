package server.core.feature.submission.application

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.submission.application.SubmissionCreateCommand
import server.core.feature.submission.application.SubmissionCreateResult
import server.core.feature.submission.application.SubmissionService
import server.core.feature.submission.domain.Submission
import server.core.feature.submission.domain.SubmissionRepository
import server.core.infra.db.transaction.Transactional
import server.core.fixture.createSubmission
import test.UnitTest

class SubmissionServiceTest : UnitTest() {
    @Test
    fun `제출 생성 시 저장한다`() = runTest {
        val transactional = mockk<Transactional>()
        val submissionRepository = mockk<SubmissionRepository>()
        val service = SubmissionService(transactional, submissionRepository)

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
        coEvery { transactional.invoke<SubmissionCreateResult>(any(), any()) } coAnswers {
            val block = secondArg<() -> SubmissionCreateResult>()
            block()
        }

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
        val transactional = mockk<Transactional>()
        val submissionRepository = mockk<SubmissionRepository>()
        val service = SubmissionService(transactional, submissionRepository)

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
        coEvery { transactional.invoke<SubmissionCreateResult>(any(), any()) } coAnswers {
            val block = secondArg<() -> SubmissionCreateResult>()
            block()
        }

        service.create(command, memberId)
    }
}
