package server.batch.common.job

internal interface CoroutineBatchJob {
    val jobName: String

    suspend fun run(parameters: Map<String, String>)
}
