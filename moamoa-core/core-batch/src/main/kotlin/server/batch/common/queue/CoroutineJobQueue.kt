package server.batch.common.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob

@Component
internal class CoroutineJobQueue(
    private val batchScope: CoroutineScope,
) {
    private val channel = Channel<Pair<CoroutineBatchJob, Map<String, String>>>(capacity = Channel.BUFFERED)

    init {
        batchScope.launch {
            for ((job, params) in channel) {
                job.run(params)
            }
        }
    }

    fun enqueue(job: CoroutineBatchJob, params: Map<String, String>) {
        runBlocking {
            channel.send(job to params)
        }
    }
}
