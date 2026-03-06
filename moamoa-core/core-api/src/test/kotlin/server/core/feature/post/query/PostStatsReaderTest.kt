package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.post.query.PostStats
import server.core.feature.post.query.PostStatsReader
import server.core.feature.post.infra.PostStatsCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class PostStatsReaderTest : UnitTest() {
    @Test
    fun `캐시된 통계가 있으면 캐시에서 조회한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val postStatsCache = mockk<PostStatsCache>()

        val cachedStats = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 3L),
            2L to PostStats(postId = 2L, viewCount = 20L, bookmarkCount = 5L)
        )

        every { postStatsCache.mGet(listOf(1L, 2L)) } returns cachedStats

        val reader = PostStatsReader(entityManager, postStatsCache, mockk<WarmupCoordinator>(relaxed = true))

        val result = reader.findPostStatsMap(listOf(1L, 2L))

        result shouldBe cachedStats
    }
}
