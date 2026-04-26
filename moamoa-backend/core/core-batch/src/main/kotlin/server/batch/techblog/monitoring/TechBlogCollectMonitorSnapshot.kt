package server.batch.techblog.monitoring

internal data class TechBlogCollectMonitorSnapshot(
    val collectRunId: Long,
    val collectExecutedAtMillis: Long,
    val collectDateKst: String,
    val updatedAtMillis: Long,
    val sources: List<TechBlogCollectSourceResult>,
    val totals: TechBlogCollectTotals,
    val lastNotifyRunId: Long?,
    val lastNotifiedAtMillis: Long?,
    val lastNotifyResultType: NotifyResultType?,
)

internal data class TechBlogCollectSourceResult(
    val techBlogId: Long,
    val techBlogKey: String,
    val techBlogTitle: String,
    val fetchStatus: FetchStatus,
    val fetchedPostCount: Int,
    val addedPostCount: Int,
    val errorType: String?,
    val errorMessage: String?,
)

internal data class TechBlogCollectTotals(
    val sourceCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val fetchedPostCount: Int,
    val addedPostCount: Int,
)

internal enum class FetchStatus {
    SUCCESS,
    FAILED,
}

internal enum class NotifyResultType {
    RESULT,
    MISSING_DATA,
}
