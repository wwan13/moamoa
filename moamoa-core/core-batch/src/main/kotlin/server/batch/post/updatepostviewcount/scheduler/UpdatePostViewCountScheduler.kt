package server.batch.post.updatepostviewcount.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.post.updatepostviewcount.job.UpdatePostViewCountCoroutineJob

@Component
internal class UpdatePostViewCountScheduler(
    private val batchScope: CoroutineScope,
    private val updatePostViewCountCoroutineJob: UpdatePostViewCountCoroutineJob,
) {

    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
    fun launchUpdatePostViewCountJob() = batchScope.launch {
        updatePostViewCountCoroutineJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}
