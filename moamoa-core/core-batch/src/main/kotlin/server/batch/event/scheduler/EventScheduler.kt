package server.batch.event.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.common.queue.BatchQueue

@Component
internal class EventScheduler(
    private val batchScope: CoroutineScope,
    private val batchQueue: BatchQueue,
    private val deleteEventOutboxJob: Job,
) {

    @Scheduled(cron = "0 30 1 * * *", zone = "Asia/Seoul")
    fun launchDeleteEventOutboxJob() = batchScope.launch {
        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()
        batchQueue.enqueue(deleteEventOutboxJob, params)
    }
}
