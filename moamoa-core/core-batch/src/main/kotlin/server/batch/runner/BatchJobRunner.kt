package server.batch.runner

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.stereotype.Service
import server.batch.common.queue.BatchQueue

@Service
internal class BatchJobRunner(
    private val batchQueue: BatchQueue,
    jobs: List<Job>,
) {
    private val jobsByName = jobs.associateBy { it.name }

    suspend fun enqueue(command: BatchJobRunCommand): BatchJobRunResult {
        val job = jobsByName[command.jobName]
            ?: throw IllegalArgumentException("지원하지 않는 job 입니다: ${command.jobName}")

        if (command.parameters.containsKey("run.id")) {
            throw IllegalArgumentException("run.id 는 자동으로 생성됩니다")
        }

        val runId = System.currentTimeMillis()
        val params = JobParametersBuilder()
            .addLong("run.id", runId)
            .apply {
                command.parameters.forEach { (key, value) ->
                    addString(key, value)
                }
            }
            .toJobParameters()

        batchQueue.enqueue(job, params)

        return BatchJobRunResult(
            jobName = command.jobName,
            runId = runId,
            parameters = command.parameters,
        )
    }
}
