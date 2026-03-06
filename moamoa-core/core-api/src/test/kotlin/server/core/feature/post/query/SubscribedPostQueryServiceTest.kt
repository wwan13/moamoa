package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.post.infra.SubscribedPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest
import java.time.LocalDateTime

class SubscribedPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 구독 게시글을 통계와 북마크와 병합한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val subscribedPostListCache = mockk<SubscribedPostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { subscribedPostListCache.get(10L, 1L) } returns listOf(
            postSummary(1L, 1L, 1L),
            postSummary(2L, 3L, 4L)
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = SubscribedPostQueryService(
            entityManager,
            subscribedPostListCache,
            bookmarkedPostReader,
            postStatsReader,
            warmupCoordinator
        )

        val result = service.findAllByConditions(
            PostQueryConditions(page = 1, size = 20, query = null),
            Passport(10L, MemberRole.USER)
        )

        result.posts[0].bookmarkCount shouldBe 11L
        result.posts[1].isBookmarked shouldBe true
    }

    private fun postSummary(id: Long, viewCount: Long, bookmarkCount: Long) = PostSummary(
        id = id,
        key = "post-$id",
        title = "title-$id",
        description = "desc-$id",
        thumbnail = "thumb-$id",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        isBookmarked = false,
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
