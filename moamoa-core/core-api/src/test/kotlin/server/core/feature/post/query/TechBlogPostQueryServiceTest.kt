package server.core.feature.post.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRole
import server.core.feature.post.infra.TechBlogPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import server.core.support.domain.ListEntry
import test.UnitTest
import java.time.LocalDateTime

class TechBlogPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 기술블로그 게시글을 병합한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val techBlogPostListCache = mockk<TechBlogPostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { techBlogPostListCache.get(1L, 1L, 20L) } returns ListEntry(
            count = 2L,
            list = listOf(
                postSummary(1L, 1L, 1L),
                postSummary(2L, 3L, 4L)
            )
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns emptyMap()
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = TechBlogPostQueryService(
            entityManager,
            techBlogPostListCache,
            bookmarkedPostReader,
            postStatsReader,
            warmupCoordinator
        )

        val result = service.findAllByConditions(
            TechBlogPostQueryConditions(techBlogId = 1L, page = 1, size = 20, category = 20L),
            Passport(10L, MemberRole.USER)
        )

        result.posts[1].isBookmarked shouldBe true
        verify(exactly = 1) { techBlogPostListCache.get(1L, 1L, 20L) }
    }

    @Test
    fun `유효하지 않은 카테고리면 예외가 발생한다`() {
        val service = TechBlogPostQueryService(
            mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true)
        )

        shouldThrow<NoSuchElementException> {
            service.findAllByConditions(
                TechBlogPostQueryConditions(techBlogId = 1L, page = 1, size = 20, category = 999L),
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
        isBookmarked = false,
        viewCount = viewCount,
        bookmarkCount = bookmarkCount,
        categoryId = 20L,
        techBlogId = 1L,
        techBlogTitle = "blog",
        techBlogIcon = "icon",
        techBlogBlogUrl = "https://blog.example.com",
        techBlogKey = "blog-key",
        techBlogSubscriptionCount = 0L,
    )
}
