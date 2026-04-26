package server.core.feature.post.infra

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get
import server.core.feature.post.query.PostSummary
import server.core.support.domain.ListEntry

@Component
class BookmarkedPostListCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "POST:LIST:BOOKMARKED:"
    private val ttlMillis: Long = 60_000L

    fun versionKey(memberId: Long) = "$prefix$memberId:VER"

    fun key(memberId: Long, version: Long, page: Long, category: Long?) =
        "$prefix$memberId:V:${version}:CATEGORY:${categoryToken(category)}:PAGE:$page:"

    fun get(memberId: Long, page: Long, category: Long?): ListEntry<PostSummary>? {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver, page, category))
    }

    fun set(memberId: Long, page: Long, category: Long?, entry: ListEntry<PostSummary>) {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId, ver, page, category), entry, ttlMillis)
    }

    fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }

    private fun categoryToken(category: Long?): Long = category ?: 0L
}
