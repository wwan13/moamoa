package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.core.feature.post.query.PostStats
import server.core.feature.post.query.PostStatsReader
import server.core.infra.cache.PostStatsCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class PostStatsReaderTest : UnitTest() {
    @Test
    fun `캐시된 통계가 있으면 캐시에서 조회한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val postStatsCache = mockk<PostStatsCache>()

        val cachedStats = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 3L),
            2L to PostStats(postId = 2L, viewCount = 20L, bookmarkCount = 5L)
        )

        every { postStatsCache.mGet(listOf(1L, 2L)) } returns cachedStats

        val reader = PostStatsReader(jdbc, postStatsCache, mockk<WarmupCoordinator>(relaxed = true))

        val result = reader.findPostStatsMap(listOf(1L, 2L))

        result shouldBe cachedStats
    }
}
