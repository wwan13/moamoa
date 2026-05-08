package server.core.feature.post.infra

import org.springframework.stereotype.Component
import server.cache.CacheMemory
import server.cache.get
import server.core.feature.post.query.PostSortType
import server.core.feature.post.query.PostSummary
import server.core.support.domain.ListEntry

@Component
class TechBlogPostListCache(
    private val cacheMemory: CacheMemory
) {
    private val prefix = "POST:LIST:TECHBLOG:"
    private val thirtyMinutes: Long = 1_800_000L

    fun key(techBlogId: Long, page: Long, category: Long?, sort: PostSortType) =
        "$prefix$techBlogId:CATEGORY:${categoryToken(category)}:SORT:${sort.name}:PAGE:$page"

    fun get(techBlogId: Long, page: Long, category: Long?, sort: PostSortType): ListEntry<PostSummary>? =
        cacheMemory.get(key(techBlogId, page, category, sort))

    fun set(techBlogId: Long, page: Long, category: Long?, sort: PostSortType, entry: ListEntry<PostSummary>) {
        cacheMemory.set(key(techBlogId, page, category, sort), entry, thirtyMinutes)
    }

    private fun categoryToken(category: Long?): Long = category ?: 0L
}
