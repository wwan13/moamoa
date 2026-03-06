package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.post.infra.PostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import server.core.support.domain.ListEntry
import test.UnitTest
import java.time.LocalDateTime

class PostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 게시글이 있으면 통계와 북마크를 병합한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )

        every { postListCache.get(1L, 20L) } returns ListEntry(
            count = 2L,
            list = basePosts
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = PostQueryService(
            entityManager = entityManager,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findByConditions(
            conditions = PostQueryConditions(page = 1, size = 20, query = null),
            passport = Passport(memberId = 10L, role = MemberRole.USER)
        )

        result.posts[0].viewCount shouldBe 10L
        result.posts[0].bookmarkCount shouldBe 11L
        result.posts[1].isBookmarked shouldBe true
    }

    private fun postSummary(
        id: Long,
        viewCount: Long,
        bookmarkCount: Long,
        isBookmarked: Boolean,
    ) = PostSummary(
        id = id,
        key = "post-$id",
        title = "title-$id",
        description = "desc-$id",
        thumbnail = "thumb-$id",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        isBookmarked = isBookmarked,
        viewCount = viewCount,
        bookmarkCount = bookmarkCount,
        techBlogId = 1L,
        techBlogTitle = "blog",
        techBlogIcon = "icon",
        techBlogBlogUrl = "https://blog.example.com",
        techBlogKey = "blog-key",
        techBlogSubscriptionCount = 0L,
    )
}
