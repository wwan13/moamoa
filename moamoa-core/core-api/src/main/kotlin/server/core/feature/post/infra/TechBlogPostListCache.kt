package server.core.feature.post.infra

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get
import server.core.feature.post.query.PostSummary
import server.core.support.domain.ListEntry

@Component
class TechBlogPostListCache(
    private val cacheMemory: CacheMemory
) {
    private val prefix = "POST:LIST:TECHBLOG:"
    private val thirtyMinutes: Long = 1_800_000L

    fun key(techBlogId: Long, page: Long) =
        "$prefix$techBlogId:PAGE:$page"

    fun get(techBlogId: Long, page: Long): ListEntry<PostSummary>? =
        cacheMemory.get(key(techBlogId, page))

    fun set(techBlogId: Long, page: Long, entry: ListEntry<PostSummary>) {
        cacheMemory.set(key(techBlogId, page), entry, thirtyMinutes)
    }
}
