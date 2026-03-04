package server.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.feature.member.command.domain.MemberRole
import server.feature.techblog.command.application.TechBlogData
import server.infra.cache.SubscribedPostListCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport
import test.UnitTest
import java.time.LocalDateTime

class SubscribedPostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 구독 게시글을 통계와 북마크와 병합한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val subscribedPostListCache = mockk<SubscribedPostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { jdbc.queryForObject(any<String>(), any<Map<String, Any>>(), Long::class.java) } returns 2L
        every { subscribedPostListCache.get(10L, 1L) } returns listOf(
            postSummary(1L, 1L, 1L),
            postSummary(2L, 3L, 4L)
        )
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = SubscribedPostQueryService(
            jdbc,
            subscribedPostListCache,
            bookmarkedPostReader,
            postStatsReader,
            warmupCoordinator
        )

        val result = service.findAllByConditions(
            PostQueryConditions(page = 1, size = 20, query = null),
            Passport(10L, MemberRole.USER)
        )

        result.meta.totalCount shouldBe 2L
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
        techBlog = TechBlogData(1L, "blog", "icon", "https://blog.example.com", "blog-key", 0L)
    )
}
