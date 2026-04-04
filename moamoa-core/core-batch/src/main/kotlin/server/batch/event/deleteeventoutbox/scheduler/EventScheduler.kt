package server.batch.event.deleteeventoutbox.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.event.deleteeventoutbox.job.DeleteEventOutboxTasklet

@Component
internal class EventScheduler(
    private val batchScope: CoroutineScope,
    private val deleteEventOutboxJob: DeleteEventOutboxTasklet,
) {

    @Scheduled(cron = "0 30 1 * * *", zone = "Asia/Seoul")
    fun launchDeleteEventOutboxJob() = batchScope.launch {
        deleteEventOutboxJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}
