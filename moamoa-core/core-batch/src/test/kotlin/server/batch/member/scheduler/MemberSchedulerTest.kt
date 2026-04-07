package server.batch.member.scheduler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.batch.core.Job as BatchJob
import server.batch.common.queue.BatchQueue
import test.UnitTest

class MemberSchedulerTest : UnitTest() {

    @Test
    fun `05시 스케줄은 generateAlarmContentJob을 큐에 등록한다`() = runBlocking {
        val batchScope = CoroutineScope(Dispatchers.Unconfined)
        val batchQueue = mockk<BatchQueue>(relaxed = true)
        val generateAlarmContentJob = mockk<BatchJob>(relaxed = true)
        val sendAlarmEmailJob = mockk<BatchJob>(relaxed = true)
        val scheduler = MemberScheduler(batchScope, batchQueue, generateAlarmContentJob, sendAlarmEmailJob)

        val launched: Job = scheduler.launchGenerateAlarmContentJob()
        launched.join()

        coVerify(exactly = 1) { batchQueue.enqueue(generateAlarmContentJob, any()) }
    }

    @Test
    fun `08시 스케줄은 sendAlarmEmailJob을 큐에 등록한다`() = runBlocking {
        val batchScope = CoroutineScope(Dispatchers.Unconfined)
        val batchQueue = mockk<BatchQueue>(relaxed = true)
        val generateAlarmContentJob = mockk<BatchJob>(relaxed = true)
        val sendAlarmEmailJob = mockk<BatchJob>(relaxed = true)
        val scheduler = MemberScheduler(batchScope, batchQueue, generateAlarmContentJob, sendAlarmEmailJob)

        val launched: Job = scheduler.launchSendAlarmEmailJob()
        launched.join()

        coVerify(exactly = 1) { batchQueue.enqueue(sendAlarmEmailJob, any()) }
    }
}
