package server.batch.techblog.notifytechblogcollectresult.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.techblog.notifytechblogcollectresult.job.NotifyTechBlogCollectResultTasklet

@Component
internal class NotifyTechBlogCollectResultScheduler(
    private val batchScope: CoroutineScope,
    private val notifyTechBlogCollectResultCoroutineJob: NotifyTechBlogCollectResultTasklet,
) {

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    fun launchNotifyTechBlogCollectResultJob() = batchScope.launch {
        notifyTechBlogCollectResultCoroutineJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}
