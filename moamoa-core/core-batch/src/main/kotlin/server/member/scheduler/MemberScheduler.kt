package server.member.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.common.queue.BatchQueue

@Component
class MemberScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    private val generateAlarmContentJob: Job,
) {

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    fun launchSyncPostBookmarkCountJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(generateAlarmContentJob, params)
    }
}