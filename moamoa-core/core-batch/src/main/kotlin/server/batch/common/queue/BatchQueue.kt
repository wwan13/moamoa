package server.batch.common.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component

@Component
internal class BatchQueue(
    private val batchScope: CoroutineScope,
    private val jobLauncher: JobLauncher,
) {
    private val channel = Channel<Pair<Job, JobParameters>>(capacity = Channel.BUFFERED)

    init {
        batchScope.launch {
            for ((job, params) in channel) {
                jobLauncher.run(job, params)
            }
        }
    }

    suspend fun enqueue(job: Job, params: JobParameters) {
        channel.send(job to params)
    }
}
