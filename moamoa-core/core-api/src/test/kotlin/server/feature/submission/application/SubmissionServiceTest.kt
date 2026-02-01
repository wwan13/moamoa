package server.feature.submission.application

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.submission.domain.Submission
import server.feature.submission.domain.SubmissionCreateEvent
import server.feature.submission.domain.SubmissionRepository
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import server.fixture.createSubmission
import test.UnitTest

class SubmissionServiceTest : UnitTest() {
    @Test
    fun `제출 생성 시 저장한다`() = runTest {
        val transactional = mockk<Transactional>()
        val submissionRepository = mockk<SubmissionRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
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
            val block = secondArg<suspend TransactionScope.() -> SubmissionCreateResult>()
            block(transactionScope)
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
        val transactionScope = mockk<TransactionScope>(relaxed = true)
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
            val block = secondArg<suspend TransactionScope.() -> SubmissionCreateResult>()
            block(transactionScope)
        }

        service.create(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is SubmissionCreateEvent &&
                        it.submissionId == savedId &&
                        it.blogTitle == command.blogTitle &&
                        it.blogUrl == command.blogUrl
                }
            )
        }
    }
}
