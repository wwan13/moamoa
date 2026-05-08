package server.core.feature.post.infra

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get
import server.core.feature.post.query.PostSortType
import server.core.feature.post.query.PostSummary
import server.core.support.domain.ListEntry

@Component
class SubscribedPostListCache(
    private val cacheMemory: CacheMemory,
) {
    private val prefix = "POST:LIST:SUBSCRIBED:"
    private val ttlMillis: Long = 60_000L

    fun versionKey(memberId: Long) = "$prefix$memberId:VER"

    fun key(memberId: Long, version: Long, page: Long, category: Long?, sort: PostSortType) =
        "$prefix$memberId:V:${version}:CATEGORY:${categoryToken(category)}:SORT:${sort.name}:PAGE:$page:"

    fun get(memberId: Long, page: Long, category: Long?, sort: PostSortType): ListEntry<PostSummary>? {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        return cacheMemory.get(key(memberId, ver, page, category, sort))
    }

    fun set(memberId: Long, page: Long, category: Long?, sort: PostSortType, entry: ListEntry<PostSummary>) {
        val ver = cacheMemory.get(versionKey(memberId)) ?: 1L
        cacheMemory.set(key(memberId, ver, page, category, sort), entry, ttlMillis)
    }

    fun evictAll(memberId: Long) {
        cacheMemory.incr(versionKey(memberId))
    }

    private fun categoryToken(category: Long?): Long = category ?: 0L
}
