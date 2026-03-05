package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.core.feature.member.domain.MemberRole
import server.core.feature.techblog.application.TechBlogData
import server.core.feature.post.infra.PostListCache
import server.core.infra.cache.WarmupCoordinator
import server.core.global.security.Passport
import test.UnitTest
import java.time.LocalDateTime

class PostQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 게시글이 있으면 통계와 북마크를 병합한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()
        val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

        every { jdbc.queryForObject(any<String>(), any<MapSqlParameterSource>(), Long::class.java) } returns 2L

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )

        every { postListCache.get(1L, 20L) } returns basePosts
        every { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        every { bookmarkedPostReader.findBookmarkedPostIdSet(10L, listOf(1L, 2L)) } returns setOf(2L)

        val service = PostQueryService(jdbc, postListCache, bookmarkedPostReader, postStatsReader, warmupCoordinator)

        val result = service.findByConditions(
            conditions = PostQueryConditions(page = 1, size = 20, query = null),
            passport = Passport(memberId = 10L, role = MemberRole.USER)
        )

        result.meta.totalCount shouldBe 2L
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
        techBlog = TechBlogData(
            id = 1L,
            title = "blog",
            icon = "icon",
            blogUrl = "https://blog.example.com",
            key = "blog-key",
            subscriptionCount = 0L
        )
    )
}
