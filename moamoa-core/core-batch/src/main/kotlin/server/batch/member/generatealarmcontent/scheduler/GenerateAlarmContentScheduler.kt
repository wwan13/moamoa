package server.batch.member.generatealarmcontent.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import server.batch.member.generatealarmcontent.job.GenerateAlarmContentCoroutineJob

@Component
internal class GenerateAlarmContentScheduler(
    private val batchScope: CoroutineScope,
    private val generateAlarmContentJob: GenerateAlarmContentCoroutineJob,
) {

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    fun launchGenerateAlarmContentJob() = batchScope.launch {
        generateAlarmContentJob.run(mapOf("run.id" to System.currentTimeMillis().toString()))
    }
}
