package server.batch.techblog.monitoring

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import server.batch.techblog.dto.TechBlogKey
import server.cache.CacheMemory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
internal class TechBlogCollectMonitorStore(
    private val cacheMemory: CacheMemory,
) {
    private val lock = Mutex()

    suspend fun recordFetchSuccess(
        runId: Long,
        techBlog: TechBlogKey,
        fetchedPostCount: Int,
    ) = lock.withLock {
        val snapshot = loadOrInitSnapshot(runId)
        val updatedSource = TechBlogCollectSourceResult(
            techBlogId = techBlog.id,
            techBlogKey = techBlog.techBlogKey,
            techBlogTitle = techBlog.title,
            fetchStatus = FetchStatus.SUCCESS,
            fetchedPostCount = fetchedPostCount,
            addedPostCount = findAddedPostCount(snapshot.sources, techBlog.id),
            errorType = null,
            errorMessage = null,
        )
        persist(snapshot.copy(sources = upsertSource(snapshot.sources, updatedSource)))
    }

    suspend fun recordFetchFailure(
        runId: Long,
        techBlog: TechBlogKey,
        throwable: Throwable,
    ) = lock.withLock {
        val snapshot = loadOrInitSnapshot(runId)
        val updatedSource = TechBlogCollectSourceResult(
            techBlogId = techBlog.id,
            techBlogKey = techBlog.techBlogKey,
            techBlogTitle = techBlog.title,
            fetchStatus = FetchStatus.FAILED,
            fetchedPostCount = 0,
            addedPostCount = findAddedPostCount(snapshot.sources, techBlog.id),
            errorType = throwable::class.simpleName ?: throwable::class.java.name,
            errorMessage = truncate(throwable.message ?: "", MAX_ERROR_MESSAGE_LENGTH),
        )
        persist(snapshot.copy(sources = upsertSource(snapshot.sources, updatedSource)))
    }

    suspend fun accumulateAddedCount(runId: Long, addedCountByTechBlogId: Map<Long, Int>) = lock.withLock {
        val snapshot = loadOrInitSnapshot(runId)
        if (addedCountByTechBlogId.isEmpty()) {
            persist(snapshot)
            return@withLock
        }

        val updatedSources = snapshot.sources.map { source ->
            val added = addedCountByTechBlogId[source.techBlogId] ?: 0
            if (added == 0) source
            else source.copy(addedPostCount = source.addedPostCount + added)
        }
        persist(snapshot.copy(sources = updatedSources))
    }

    suspend fun markNotified(notifyRunId: Long, nowMillis: Long, resultType: NotifyResultType) = lock.withLock {
        val snapshot = cacheMemory.get(KEY, TechBlogCollectMonitorSnapshot::class.java)
            ?: defaultSnapshot()

        persist(
            snapshot.copy(
                lastNotifyRunId = notifyRunId,
                lastNotifiedAtMillis = nowMillis,
                lastNotifyResultType = resultType,
            )
        )
    }

    suspend fun getLatest(): TechBlogCollectMonitorSnapshot? =
        cacheMemory.get(KEY, TechBlogCollectMonitorSnapshot::class.java)

    private suspend fun loadOrInitSnapshot(runId: Long): TechBlogCollectMonitorSnapshot {
        val current = cacheMemory.get(KEY, TechBlogCollectMonitorSnapshot::class.java)
        if (current == null || current.collectRunId != runId) {
            return TechBlogCollectMonitorSnapshot(
                collectRunId = runId,
                collectExecutedAtMillis = runId,
                collectDateKst = LocalDate.ofInstant(Instant.ofEpochMilli(runId), ZONE_KST).format(DATE_FORMATTER),
                updatedAtMillis = nowMillis(),
                sources = emptyList(),
                totals = TechBlogCollectTotals(0, 0, 0, 0, 0),
                lastNotifyRunId = null,
                lastNotifiedAtMillis = null,
                lastNotifyResultType = null,
            )
        }

        return current
    }

    private suspend fun persist(snapshot: TechBlogCollectMonitorSnapshot) {
        val updated = snapshot.copy(
            updatedAtMillis = nowMillis(),
            totals = calculateTotals(snapshot.sources),
            sources = snapshot.sources.sortedBy { it.techBlogId }
        )
        cacheMemory.set(KEY, updated, null)
    }

    private fun calculateTotals(sources: List<TechBlogCollectSourceResult>): TechBlogCollectTotals {
        val successCount = sources.count { it.fetchStatus == FetchStatus.SUCCESS }
        val failureCount = sources.count { it.fetchStatus == FetchStatus.FAILED }

        return TechBlogCollectTotals(
            sourceCount = sources.size,
            successCount = successCount,
            failureCount = failureCount,
            fetchedPostCount = sources.sumOf { it.fetchedPostCount },
            addedPostCount = sources.sumOf { it.addedPostCount },
        )
    }

    private fun upsertSource(
        sources: List<TechBlogCollectSourceResult>,
        source: TechBlogCollectSourceResult
    ): List<TechBlogCollectSourceResult> {
        val others = sources.filterNot { it.techBlogId == source.techBlogId }
        return others + source
    }

    private fun findAddedPostCount(sources: List<TechBlogCollectSourceResult>, techBlogId: Long): Int =
        sources.firstOrNull { it.techBlogId == techBlogId }?.addedPostCount ?: 0

    private fun defaultSnapshot(): TechBlogCollectMonitorSnapshot {
        val now = nowMillis()
        val nowDateKst = LocalDate.ofInstant(Instant.ofEpochMilli(now), ZONE_KST).format(DATE_FORMATTER)

        return TechBlogCollectMonitorSnapshot(
            collectRunId = 0,
            collectExecutedAtMillis = 0,
            collectDateKst = nowDateKst,
            updatedAtMillis = now,
            sources = emptyList(),
            totals = TechBlogCollectTotals(0, 0, 0, 0, 0),
            lastNotifyRunId = null,
            lastNotifiedAtMillis = null,
            lastNotifyResultType = null,
        )
    }

    private fun nowMillis(): Long = System.currentTimeMillis()

    private fun truncate(value: String, maxLength: Int): String {
        if (value.length <= maxLength) return value
        return value.take(maxLength)
    }

    companion object {
        const val KEY = "BATCH:TECH_BLOG:COLLECT:MONITOR"
        private const val MAX_ERROR_MESSAGE_LENGTH = 200
        private val ZONE_KST = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
