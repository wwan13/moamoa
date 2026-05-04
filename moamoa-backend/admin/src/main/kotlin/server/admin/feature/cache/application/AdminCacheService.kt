package server.admin.feature.cache.application

import org.springframework.stereotype.Service
import server.cache.CacheMemory

@Service
internal class AdminCacheService(
    private val cacheMemory: CacheMemory,
) {

    fun findAll(): List<AdminCacheSummary> =
        cacheSpecs.map { it.toSummary() }

    fun evict(cacheKey: String): AdminCacheEvictResult {
        val spec = cacheSpecs.find { it.key == cacheKey }
            ?: throw NoSuchElementException("지원하지 않는 캐시입니다: $cacheKey")

        check(spec.evictable) {
            spec.unsupportedReason ?: "현재 캐시는 운영자 evict를 지원하지 않습니다."
        }

        spec.evict(cacheMemory)
        return spec.toEvictResult()
    }

    fun evictAll(): AdminCacheEvictAllResult {
        val evictable = cacheSpecs.filter { it.evictable }
        val skipped = cacheSpecs.filterNot { it.evictable }

        evictable.forEach { it.evict(cacheMemory) }

        return AdminCacheEvictAllResult(
            evicted = evictable.map { it.toEvictResult() },
            skipped = skipped.map { it.toSummary() },
        )
    }

    private data class CacheSpec(
        val key: String,
        val name: String,
        val description: String,
        val target: String,
        val evictionStrategy: String,
        val evictable: Boolean,
        val unsupportedReason: String? = null,
        val evict: (CacheMemory) -> Unit,
    ) {
        fun toSummary(): AdminCacheSummary = AdminCacheSummary(
            key = key,
            name = name,
            description = description,
            target = target,
            evictionStrategy = evictionStrategy,
            evictable = evictable,
            unsupportedReason = unsupportedReason,
        )

        fun toEvictResult(): AdminCacheEvictResult = AdminCacheEvictResult(
            key = key,
            name = name,
            evictionStrategy = evictionStrategy,
        )
    }

    companion object {
        private val cacheSpecs = listOf(
            CacheSpec(
                key = "techblog-summary",
                name = "기술블로그 요약",
                description = "기술블로그별 구독/게시글 요약 캐시",
                target = "core-api",
                evictionStrategy = "prefix",
                evictable = true,
                evict = { it.evictByPrefix("TECHBLOG:SUMMARY:") },
            ),
            CacheSpec(
                key = "techblog-list",
                name = "기술블로그 목록",
                description = "공개 기술블로그 기본 목록 캐시",
                target = "core-api",
                evictionStrategy = "exact_key",
                evictable = true,
                evict = { it.evict("TECHBLOG:BASE:LIST") },
            ),
            CacheSpec(
                key = "subscription-list",
                name = "구독 목록",
                description = "회원별 기술블로그 구독 목록 버전 캐시",
                target = "core-api",
                evictionStrategy = "versioned_prefix",
                evictable = true,
                evict = { it.evictByPrefix("TECHBLOG:SUBSCRIPTION:ALL:") },
            ),
            CacheSpec(
                key = "post-list",
                name = "게시글 목록",
                description = "카테고리별 공개 게시글 목록 캐시",
                target = "core-api",
                evictionStrategy = "prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:LIST:CATEGORY:") },
            ),
            CacheSpec(
                key = "subscribed-post-list",
                name = "구독 게시글 목록",
                description = "회원별 구독 피드 버전 캐시",
                target = "core-api",
                evictionStrategy = "versioned_prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:LIST:SUBSCRIBED:") },
            ),
            CacheSpec(
                key = "bookmarked-post-list",
                name = "북마크 게시글 목록",
                description = "회원별 북마크 목록 버전 캐시",
                target = "core-api",
                evictionStrategy = "versioned_prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:LIST:BOOKMARKED:") },
            ),
            CacheSpec(
                key = "bookmarked-post-id-set",
                name = "북마크 게시글 ID 집합",
                description = "회원별 전체 북마크 ID 버전 캐시",
                target = "core-api",
                evictionStrategy = "versioned_prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:BOOKMARKED:ALL:") },
            ),
            CacheSpec(
                key = "post-stats",
                name = "게시글 통계",
                description = "게시글별 조회/북마크 통계 캐시",
                target = "core-api",
                evictionStrategy = "prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:STATS:") },
            ),
            CacheSpec(
                key = "techblog-post-list",
                name = "기술블로그별 게시글 목록",
                description = "기술블로그 상세의 게시글 목록 캐시",
                target = "core-api",
                evictionStrategy = "prefix",
                evictable = true,
                evict = { it.evictByPrefix("POST:LIST:TECHBLOG:") },
            ),
        )
    }
}
