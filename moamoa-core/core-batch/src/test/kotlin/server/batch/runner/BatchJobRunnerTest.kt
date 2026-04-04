package server.batch.runner

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.batch.common.job.CoroutineBatchJob
import server.batch.common.queue.CoroutineJobQueue
import test.UnitTest

class BatchJobRunnerTest : UnitTest() {

    @Test
    fun `등록된 coroutine job을 큐에 enqueue한다`() {
        val jobQueue = mockk<CoroutineJobQueue>(relaxed = true)
        val coroutineJob = mockk<CoroutineBatchJob>()
        every { coroutineJob.jobName } returns "updatePostViewCountJob"
        val runner = BatchJobRunner(
            jobQueue = jobQueue,
            coroutineJobs = listOf(coroutineJob)
        )

        val result = runner.enqueue(
            BatchJobRunCommand(
                jobName = "updatePostViewCountJob",
                parameters = mapOf("k" to "v")
            )
        )

        result.jobName shouldBe "updatePostViewCountJob"
        result.parameters shouldBe mapOf("k" to "v")
        verify(exactly = 1) { jobQueue.enqueue(coroutineJob, match { it["k"] == "v" && it.containsKey("run.id") }) }
    }

    @Test
    fun `지원하지 않는 job이면 예외를 던진다`() {
        val jobQueue = mockk<CoroutineJobQueue>(relaxed = true)
        val coroutineJob = mockk<CoroutineBatchJob>()
        every { coroutineJob.jobName } returns "syncBookmarkCountJob"
        coEvery { coroutineJob.run(any()) } returns Unit
        val runner = BatchJobRunner(
            jobQueue = jobQueue,
            coroutineJobs = listOf(coroutineJob)
        )

        var thrown: Throwable? = null
        runCatching {
            runner.enqueue(
                BatchJobRunCommand(
                    jobName = "unknownJob",
                    parameters = mapOf("requestedBy" to "manual")
                )
            )
        }.onFailure { thrown = it }

        (thrown is IllegalArgumentException) shouldBe true
        verify(exactly = 0) { jobQueue.enqueue(any(), any()) }
    }

    @Test
    fun `run id를 직접 넣으면 예외를 던진다`() {
        val jobQueue = mockk<CoroutineJobQueue>(relaxed = true)
        val coroutineJob = mockk<CoroutineBatchJob>()
        every { coroutineJob.jobName } returns "syncBookmarkCountJob"
        val runner = BatchJobRunner(
            jobQueue = jobQueue,
            coroutineJobs = listOf(coroutineJob)
        )

        var thrown: Throwable? = null
        runCatching {
            runner.enqueue(
                BatchJobRunCommand(
                jobName = "syncBookmarkCountJob",
                    parameters = mapOf("run.id" to "1")
                )
            )
        }.onFailure { thrown = it }

        (thrown is IllegalArgumentException) shouldBe true
        verify(exactly = 0) { jobQueue.enqueue(any(), any()) }
    }
}
