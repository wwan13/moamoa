package server.batch.member.sendalarmemail.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.member.sendalarmemail.job.SendAlarmEmailTasklet

@Component
internal class SendAlarmEmailScheduler(
    private val batchScope: CoroutineScope,
    private val sendAlarmEmailJob: SendAlarmEmailTasklet,
) {

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    fun launchSendAlarmEmailJob() = batchScope.launch {
        sendAlarmEmailJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}
