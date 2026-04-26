package server.batch.member.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.common.queue.BatchQueue

@Component
internal class MemberScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    @param:Qualifier("generateAlarmContentJob")
    private val generateAlarmContentJob: Job,
    @param:Qualifier("sendAlarmEmailJob")
    private val sendAlarmEmailJob: Job,
) {

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    fun launchGenerateAlarmContentJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(generateAlarmContentJob, params)
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    fun launchSendAlarmEmailJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .toJobParameters()
        batchQueue.enqueue(sendAlarmEmailJob, params)
    }
}
