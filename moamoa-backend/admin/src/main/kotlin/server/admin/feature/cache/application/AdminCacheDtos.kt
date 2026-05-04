package server.admin.feature.cache.application

internal data class AdminCacheSummary(
    val key: String,
    val name: String,
    val description: String,
    val target: String,
    val evictionStrategy: String,
    val evictable: Boolean,
    val unsupportedReason: String? = null,
)

internal data class AdminCacheEvictResult(
    val key: String,
    val name: String,
    val evictionStrategy: String,
)

internal data class AdminCacheEvictAllResult(
    val evicted: List<AdminCacheEvictResult>,
    val skipped: List<AdminCacheSummary>,
)
