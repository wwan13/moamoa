package server.core.feature.post.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.post.infra.BookmarkedPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import server.core.support.domain.ListEntry
import test.UnitTest
import java.time.LocalDateTime

class BookmarkedPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 북마크 게시글을 통계와 병합한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { bookmarkedPostListCache.get(10L, 1L, 10L) } returns ListEntry(
            count = 2L,
            list = listOf(
                postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L),
                postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L)
            )
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )

        val service = BookmarkedPostQueryService(
            entityManager,
            bookmarkedPostListCache,
            postStatsReader,
            warmupCoordinator
        )

        val result = service.findAllByConditions(
            PostQueryConditions(page = 1, size = 20, query = null, category = 10L),
            Passport(10L, MemberRole.USER)
        )

        result.posts[0].bookmarkCount shouldBe 11L
        result.posts.all { it.isBookmarked } shouldBe true
        verify(exactly = 1) { bookmarkedPostListCache.get(10L, 1L, 10L) }
    }

    @Test
    fun `유효하지 않은 카테고리면 예외가 발생한다`() {
        val service = BookmarkedPostQueryService(
            mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk(relaxed = true)
        )

        shouldThrow<NoSuchElementException> {
            service.findAllByConditions(
                PostQueryConditions(page = 1, size = 20, query = null, category = 999L),
                Passport(10L, MemberRole.USER)
            )
        }
    }

    private fun postSummary(id: Long, viewCount: Long, bookmarkCount: Long) = PostSummary(
        id = id,
        key = "post-$id",
        title = "title-$id",
        description = "desc-$id",
        thumbnail = "thumb-$id",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        isBookmarked = true,
        viewCount = viewCount,
        bookmarkCount = bookmarkCount,
        categoryId = 10L,
        techBlogId = 1L,
        techBlogTitle = "blog",
        techBlogIcon = "icon",
        techBlogBlogUrl = "https://blog.example.com",
        techBlogKey = "blog-key",
        techBlogSubscriptionCount = 0L,
    )
}
