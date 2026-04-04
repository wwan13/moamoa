package server.batch.techblog.collecttechblogpost.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.techblog.collecttechblogpost.job.CollectTechBlogPostCoroutineJob

@Component
internal class CollectTechBlogPostScheduler(
    private val batchScope: CoroutineScope,
    private val collectTechBlogPostCoroutineJob: CollectTechBlogPostCoroutineJob,
) {

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun launchCollectTechBlogPostJob() = batchScope.launch {
        collectTechBlogPostCoroutineJob.run(
            mapOf(
                "run.id" to System.currentTimeMillis().toString(),
                "postLimit" to "10"
            )
        )
    }
}
