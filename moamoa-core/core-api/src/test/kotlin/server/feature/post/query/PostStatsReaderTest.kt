package server.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import reactor.core.publisher.Flux
import server.infra.cache.PostStatsCache
import test.UnitTest
import java.util.function.BiFunction

class PostStatsReaderTest : UnitTest() {
    @Test
    fun `캐시된 통계가 있으면 캐시에서 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postStatsCache = mockk<PostStatsCache>()

        val postIds = listOf(1L, 2L)
        val cachedStats = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 3L),
            2L to PostStats(postId = 2L, viewCount = 20L, bookmarkCount = 5L)
        )

        coEvery { postStatsCache.mGet(postIds) } returns cachedStats

        val reader = PostStatsReader(
            databaseClient = databaseClient,
            postStatsCache = postStatsCache,
            cacheWarmupScope = this
        )

        val result = reader.findPostStatsMap(postIds)

        result shouldBe cachedStats
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
        coVerify(exactly = 0) { postStatsCache.mSet(any()) }
    }

    @Test
    fun `캐시된 통계가 없으면 조회 후 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postStatsCache = mockk<PostStatsCache>()

        val postIds = listOf(10L, 20L, 30L)

        val fetchSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<PostStats>>()

        val dbStats = listOf(
            PostStats(postId = 10L, viewCount = 100L, bookmarkCount = 7L),
            PostStats(postId = 30L, viewCount = 300L, bookmarkCount = 21L)
        )
        val dbStatsMap = dbStats.associateBy { it.postId }

        coEvery { postStatsCache.mGet(postIds) } returns mapOf(
            10L to null,
            20L to null,
            30L to null
        )
        coEvery { postStatsCache.mSet(dbStatsMap) } returns Unit

        every { databaseClient.sql(any<String>()) } returns fetchSpec

        every { fetchSpec.bind("id0", 10L) } returns fetchSpec
        every { fetchSpec.bind("id1", 20L) } returns fetchSpec
        every { fetchSpec.bind("id2", 30L) } returns fetchSpec
        every { fetchSpec.map(any<BiFunction<Row, RowMetadata, PostStats>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.all() } returns Flux.fromIterable(dbStats)

        val reader = PostStatsReader(
            databaseClient = databaseClient,
            postStatsCache = postStatsCache,
            cacheWarmupScope = this
        )

        val result = reader.findPostStatsMap(postIds)
        advanceUntilIdle()

        result shouldBe dbStatsMap
        coVerify(exactly = 1) { postStatsCache.mSet(dbStatsMap) }
    }
}
