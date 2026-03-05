package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.core.feature.member.domain.MemberRole
import server.core.feature.techblog.application.TechBlogData
import server.core.infra.cache.TechBlogPostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest
import java.time.LocalDateTime

class TechBlogPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 기술블로그 게시글을 병합한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val techBlogPostListCache = mockk<TechBlogPostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { jdbc.queryForObject(any<String>(), any<Map<String, Any>>(), Long::class.java) } returns 2L
        every { techBlogPostListCache.get(1L, 1L) } returns listOf(
            postSummary(1L, 1L, 1L),
            postSummary(2L, 3L, 4L)
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns emptyMap()
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = TechBlogPostQueryService(
            jdbc,
            techBlogPostListCache,
            bookmarkedPostReader,
            postStatsReader,
            warmupCoordinator
        )

        val result = service.findAllByConditions(
            TechBlogPostQueryConditions(techBlogId = 1L, page = 1, size = 20),
            Passport(10L, MemberRole.USER)
        )

        result.meta.totalCount shouldBe 2L
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
        techBlog = TechBlogData(1L, "blog", "icon", "https://blog.example.com", "blog-key", 0L)
    )
}
