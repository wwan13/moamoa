package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.core.feature.member.domain.MemberRole
import server.core.feature.techblog.application.TechBlogData
import server.core.infra.cache.BookmarkedPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest
import java.time.LocalDateTime

class BookmarkedPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 북마크 게시글을 통계와 병합한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val bookmarkedPostListCache = mockk<BookmarkedPostListCache>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { jdbc.queryForObject(any<String>(), any<Map<String, Any>>(), Long::class.java) } returns 2L
        every { bookmarkedPostListCache.get(10L, 1L) } returns listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L)
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )

        val service = BookmarkedPostQueryService(jdbc, bookmarkedPostListCache, postStatsReader, warmupCoordinator)

        val result = service.findAllByConditions(
            PostQueryConditions(page = 1, size = 20, query = null),
            Passport(10L, MemberRole.USER)
        )

        result.meta.totalCount shouldBe 2L
        result.posts[0].bookmarkCount shouldBe 11L
        result.posts.all { it.isBookmarked } shouldBe true
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
        techBlog = TechBlogData(1L, "blog", "icon", "https://blog.example.com", "blog-key", 0L)
    )
}
