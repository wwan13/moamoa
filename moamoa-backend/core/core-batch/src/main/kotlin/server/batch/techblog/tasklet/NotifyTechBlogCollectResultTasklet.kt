package server.batch.techblog.tasklet

import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Value
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import server.WebhookSender
import server.batch.techblog.monitoring.FetchStatus
import server.batch.techblog.monitoring.NotifyResultType
import server.batch.techblog.monitoring.TechBlogCollectMonitorSnapshot
import server.batch.techblog.monitoring.TechBlogCollectSourceResult
import server.batch.techblog.monitoring.TechBlogCollectMonitorStore
import server.content.WebhookContent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@StepScope
@Component
internal class NotifyTechBlogCollectResultTasklet(
    private val monitorStore: TechBlogCollectMonitorStore,
    private val webhookSender: WebhookSender,
    @field:Value("#{jobParameters['run.id']}") private val runId: Long?,
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val notifyRunId = runId ?: System.currentTimeMillis()
        val now = Instant.ofEpochMilli(notifyRunId).atZone(ZONE_KST)
        val today = now.toLocalDate().format(DATE_FORMATTER)

        runBlocking {
            val snapshot = monitorStore.getLatest()
            if (snapshot == null || snapshot.collectDateKst != today) {
                val content = missingDataContent(snapshot, now.format(DATE_TIME_FORMATTER))
                webhookSender.send(content)
                monitorStore.markNotified(notifyRunId, notifyRunId, NotifyResultType.MISSING_DATA)
            } else {
                val content = resultContent(snapshot, now.format(DATE_TIME_FORMATTER))
                webhookSender.send(content)
                if (snapshot.totals.failureCount > 0) {
                    webhookSender.send(errorContent(snapshot, now.format(DATE_TIME_FORMATTER)))
                }
                monitorStore.markNotified(notifyRunId, notifyRunId, NotifyResultType.RESULT)
            }
        }

        return RepeatStatus.FINISHED
    }

    private fun missingDataContent(
        snapshot: TechBlogCollectMonitorSnapshot?,
        nowKst: String,
    ): WebhookContent.Batch {
        val lastExecutedAt = snapshot?.collectExecutedAtMillis
            ?.takeIf { it > 0 }
            ?.let { millisToKst(it) }
            ?: "없음"

        return WebhookContent.Batch(
            title = "collectTechBlogPostJob 결과 없음",
            description = "오늘(한국 시간) collectTechBlogPostJob 실행 결과를 찾지 못했습니다.",
            fields = listOf(
                "현재 시각(KST)" to nowKst,
                "마지막 수집 실행 시각(KST)" to lastExecutedAt,
            )
        )
    }

    private fun resultContent(
        snapshot: TechBlogCollectMonitorSnapshot,
        nowKst: String,
    ): WebhookContent.Batch {
        val summary = listOf(
            "runId=${snapshot.collectRunId}",
            "collectExecutedAt=${millisToKst(snapshot.collectExecutedAtMillis)}",
            "source=${snapshot.totals.sourceCount}",
            "success=${snapshot.totals.successCount}",
            "failed=${snapshot.totals.failureCount}",
            "fetched=${snapshot.totals.fetchedPostCount}",
            "added=${snapshot.totals.addedPostCount}",
        ).joinToString(", ")

        val description = TechBlogCollectResultDescriptionBuilder.build(summary, snapshot)

        return WebhookContent.Batch(
            title = "collectTechBlogPostJob 결과",
            description = description,
            fields = listOf(
                "수집 날짜(KST)" to snapshot.collectDateKst,
                "알림 시각(KST)" to nowKst,
            )
        )
    }

    private fun errorContent(
        snapshot: TechBlogCollectMonitorSnapshot,
        nowKst: String,
    ): WebhookContent.Error {
        val failedSources = snapshot.sources.filter { it.fetchStatus == FetchStatus.FAILED }
        val description = TechBlogCollectErrorDescriptionBuilder.build(snapshot, failedSources)

        return WebhookContent.Error(
            title = "collectTechBlogPostJob fetch 실패",
            description = description,
            fields = listOf(
                "수집 날짜(KST)" to snapshot.collectDateKst,
                "알림 시각(KST)" to nowKst,
                "실패 개수" to failedSources.size.toString(),
            )
        )
    }

    private fun millisToKst(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZONE_KST).format(DATE_TIME_FORMATTER)

    companion object {
        private val ZONE_KST = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}

internal object TechBlogCollectErrorDescriptionBuilder {
    private const val MAX_DESCRIPTION_LENGTH = 3500

    fun build(
        snapshot: TechBlogCollectMonitorSnapshot,
        failedSources: List<TechBlogCollectSourceResult>,
    ): String {
        val summary = listOf(
            "runId=${snapshot.collectRunId}",
            "collectExecutedAt=${snapshot.collectExecutedAtMillis}",
            "failed=${failedSources.size}"
        ).joinToString(", ")

        if (summary.length >= MAX_DESCRIPTION_LENGTH) return summary.take(MAX_DESCRIPTION_LENGTH)
        if (failedSources.isEmpty()) return summary

        val result = StringBuilder(summary).append("\n\nFAILED SOURCES")
        var includedCount = 0

        for (source in failedSources) {
            val line = "\n- ${source.techBlogTitle}: ${errorText(source)}"
            if (result.length + line.length > MAX_DESCRIPTION_LENGTH) {
                return appendOmittedSuffix(result, failedSources.size - includedCount)
            }
            result.append(line)
            includedCount += 1
        }

        val omittedCount = failedSources.size - includedCount
        if (omittedCount > 0) return appendOmittedSuffix(result, omittedCount)
        return result.toString()
    }

    private fun errorText(source: TechBlogCollectSourceResult): String {
        val type = source.errorType?.takeIf { it.isNotBlank() } ?: "Unknown"
        val message = source.errorMessage?.takeIf { it.isNotBlank() } ?: "no-message"
        return "$type:$message"
    }

    private fun appendOmittedSuffix(result: StringBuilder, omittedCount: Int): String {
        if (omittedCount <= 0) return result.toString()

        val suffix = "\n... and $omittedCount more sources"
        if (result.length + suffix.length <= MAX_DESCRIPTION_LENGTH) {
            return result.append(suffix).toString()
        }

        val keepLength = (MAX_DESCRIPTION_LENGTH - suffix.length).coerceAtLeast(0)
        val trimmed = result.toString().take(keepLength)
        return trimmed + suffix
    }
}

internal object TechBlogCollectResultDescriptionBuilder {
    private const val MAX_DESCRIPTION_LENGTH = 3500

    fun build(summary: String, snapshot: TechBlogCollectMonitorSnapshot): String {
        val failedSources = snapshot.sources.filter { it.fetchStatus == FetchStatus.FAILED }
        val successSources = snapshot.sources.filter { it.fetchStatus == FetchStatus.SUCCESS }
        if (failedSources.isEmpty()) {
            return buildAllSuccess(summary, successSources)
        }

        return buildWithLimit(
            summary = summary,
            failedLines = failedSources.map { failLine(it) },
            successLines = successSources.map { successLine(it) }
        )
    }

    private fun buildAllSuccess(
        summary: String,
        successSources: List<TechBlogCollectSourceResult>,
    ): String {
        if (summary.length >= MAX_DESCRIPTION_LENGTH) return summary.take(MAX_DESCRIPTION_LENGTH)

        val addedLines = successSources
            .filter { it.addedPostCount > 0 }
            .map { "${it.techBlogTitle}: added=${it.addedPostCount}" }

        val result = StringBuilder(summary)
        val successMessage = "\n\n모든 tech blog fetch가 성공했습니다."
        if (result.length + successMessage.length > MAX_DESCRIPTION_LENGTH) {
            return result.toString()
        }
        result.append(successMessage)

        if (addedLines.isEmpty()) return result.toString()

        val sectionHeader = "\n\nADDED"
        if (result.length + sectionHeader.length > MAX_DESCRIPTION_LENGTH) {
            return result.toString()
        }
        result.append(sectionHeader)

        var included = 0
        for (line in addedLines) {
            val piece = "\n- $line"
            if (result.length + piece.length > MAX_DESCRIPTION_LENGTH) {
                val omitted = addedLines.size - included
                return appendOmittedSuffix(result, omitted)
            }
            result.append(piece)
            included += 1
        }

        val omitted = addedLines.size - included
        if (omitted > 0) return appendOmittedSuffix(result, omitted)
        return result.toString()
    }

    private fun buildWithLimit(
        summary: String,
        failedLines: List<String>,
        successLines: List<String>
    ): String {
        if (summary.length >= MAX_DESCRIPTION_LENGTH) return summary.take(MAX_DESCRIPTION_LENGTH)

        val totalSourceCount = failedLines.size + successLines.size
        if (totalSourceCount == 0) return summary

        val result = StringBuilder(summary)
        var includedSourceCount = 0
        val sections = listOf(
            "FAIL" to failedLines,
            "SUCCESS" to successLines
        )

        for ((sectionTitle, lines) in sections) {
            if (lines.isEmpty()) continue

            var headerWritten = false
            for (line in lines) {
                val piece = if (!headerWritten) "\n\n$sectionTitle\n- $line" else "\n- $line"
                if (result.length + piece.length > MAX_DESCRIPTION_LENGTH) {
                    return appendOmittedSuffix(result, totalSourceCount - includedSourceCount)
                }

                result.append(piece)
                includedSourceCount += 1
                headerWritten = true
            }
        }

        val omittedCount = totalSourceCount - includedSourceCount
        if (omittedCount > 0) return appendOmittedSuffix(result, omittedCount)

        return result.toString()
    }

    private fun failLine(source: TechBlogCollectSourceResult): String {
        val error = buildErrorText(source)
        return "${source.techBlogTitle}: added=${source.addedPostCount}, error=$error"
    }

    private fun successLine(source: TechBlogCollectSourceResult): String =
        "${source.techBlogTitle}: added=${source.addedPostCount}"

    private fun buildErrorText(source: TechBlogCollectSourceResult): String {
        val type = source.errorType?.takeIf { it.isNotBlank() } ?: "Unknown"
        val message = source.errorMessage?.takeIf { it.isNotBlank() }
        return if (message == null) type else "$type:$message"
    }

    private fun appendOmittedSuffix(result: StringBuilder, omittedCount: Int): String {
        if (omittedCount <= 0) return result.toString()

        val suffix = "\n... and $omittedCount more sources"
        if (result.length + suffix.length <= MAX_DESCRIPTION_LENGTH) {
            return result.append(suffix).toString()
        }

        val keepLength = (MAX_DESCRIPTION_LENGTH - suffix.length).coerceAtLeast(0)
        val trimmed = result.toString().take(keepLength)
        return trimmed + suffix
    }
}
