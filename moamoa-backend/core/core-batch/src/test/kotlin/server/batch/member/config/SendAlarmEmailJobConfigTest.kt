package server.batch.member.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.repository.JobRepository
import org.springframework.transaction.PlatformTransactionManager
import io.mockk.mockk
import server.batch.member.tasklet.SendAlarmEmailTasklet
import test.UnitTest

class SendAlarmEmailJobConfigTest : UnitTest() {

    @Test
    fun `tasklet으로 step과 job을 구성한다`() {
        val config = SendAlarmEmailJobConfig()
        val jobRepository = mockk<JobRepository>(relaxed = true)
        val txManager = mockk<PlatformTransactionManager>(relaxed = true)
        val tasklet = mockk<SendAlarmEmailTasklet>(relaxed = true)

        val step = config.sendAlarmEmailStep(jobRepository, txManager, tasklet)
        val job = config.sendAlarmEmailJob(jobRepository, step)

        step.name shouldBe "sendAlarmEmailStep"
        job.name shouldBe "sendAlarmEmailJob"
    }
}
