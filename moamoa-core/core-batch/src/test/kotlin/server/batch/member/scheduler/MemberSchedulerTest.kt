package server.batch.member.generatealarmcontent.scheduler

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import server.batch.member.generatealarmcontent.job.GenerateAlarmContentCoroutineJob
import server.batch.member.sendalarmemail.job.SendAlarmEmailTasklet
import server.batch.member.sendalarmemail.scheduler.SendAlarmEmailScheduler
import test.UnitTest

class MemberSchedulerTest : UnitTest() {

    @Test
    fun `05시 스케줄은 generateAlarmContentJob을 큐에 등록한다`() = runBlocking {
        val batchScope = CoroutineScope(Dispatchers.Unconfined)
        val generateAlarmContentJob = mockk<GenerateAlarmContentCoroutineJob>(relaxed = true)
        val scheduler = GenerateAlarmContentScheduler(batchScope, generateAlarmContentJob)

        val launched: Job = scheduler.launchGenerateAlarmContentJob()
        launched.join()

        coVerify(exactly = 1) { generateAlarmContentJob.run(any()) }
    }

    @Test
    fun `08시 스케줄은 sendAlarmEmailJob을 큐에 등록한다`() = runBlocking {
        val batchScope = CoroutineScope(Dispatchers.Unconfined)
        val sendAlarmEmailJob = mockk<SendAlarmEmailTasklet>(relaxed = true)
        val scheduler = SendAlarmEmailScheduler(batchScope, sendAlarmEmailJob)

        val launched: Job = scheduler.launchSendAlarmEmailJob()
        launched.join()

        coVerify(exactly = 1) { sendAlarmEmailJob.run(any()) }
    }
}
