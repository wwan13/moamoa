package server.batch.runner

import org.springframework.stereotype.Service
import server.batch.common.job.CoroutineBatchJob
import server.batch.common.queue.CoroutineJobQueue

@Service
internal class BatchJobRunner(
    private val jobQueue: CoroutineJobQueue,
    coroutineJobs: List<CoroutineBatchJob>,
) {
    private val coroutineJobsByName = coroutineJobs.associateBy { it.jobName }

    fun enqueue(command: BatchJobRunCommand): BatchJobRunResult {
        if (command.parameters.containsKey("run.id")) {
            throw IllegalArgumentException("run.id 는 자동으로 생성됩니다")
        }

        val runId = System.currentTimeMillis()
        val coroutineJob = coroutineJobsByName[command.jobName]
            ?: throw IllegalArgumentException("지원하지 않는 job 입니다: ${command.jobName}")

        val params = buildMap {
            put("run.id", runId.toString())
            putAll(command.parameters)
        }
        jobQueue.enqueue(coroutineJob, params)

        return BatchJobRunResult(
            jobName = command.jobName,
            runId = runId,
            parameters = command.parameters,
        )
    }
}
