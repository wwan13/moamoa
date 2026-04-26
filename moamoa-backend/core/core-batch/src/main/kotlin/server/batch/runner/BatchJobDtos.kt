package server.batch.runner

internal data class BatchJobRunCommand(
    val jobName: String,
    val parameters: Map<String, String> = emptyMap(),
)

internal data class BatchJobRunResult(
    val jobName: String,
    val runId: Long,
    val parameters: Map<String, String>,
)
