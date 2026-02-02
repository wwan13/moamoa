package server.batch.runner

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import server.batch.config.BatchRunKeyProperties

@RestController
@RequestMapping("/api/batch")
internal class BatchJobController(
    private val batchJobRunner: BatchJobRunner,
    private val batchRunKeyProperties: BatchRunKeyProperties,
) {

    @PostMapping("/run")
    suspend fun run(
        @RequestHeader("X-Batch-Run-Key", required = false) batchRunKey: String?,
        @RequestBody request: BatchJobRunCommand,
    ): ResponseEntity<BatchJobRunResult> {
        if (batchRunKey == null || batchRunKey != batchRunKeyProperties.runKey) {
            throw IllegalArgumentException("BATCH_RUN_KEY_INVALID")
        }
        val response = batchJobRunner.enqueue(request)
        return ResponseEntity.ok(response)
    }
}
