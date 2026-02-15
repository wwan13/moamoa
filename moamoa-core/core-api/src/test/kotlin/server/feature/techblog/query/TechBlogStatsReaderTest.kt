package server.feature.techblog.query

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import server.infra.cache.TechBlogSummaryCache
import server.infra.cache.WarmupCoordinator
import test.UnitTest
import java.util.function.BiFunction

class TechBlogStatsReaderTest : UnitTest() {
    private val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

    init {
        every { warmupCoordinator.launchIfAbsent(any(), any()) } answers {
            runBlocking { secondArg<suspend () -> Unit>().invoke() }
        }
    }
    @Test
    fun `캐시된 통계가 있으면 캐시에서 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val techBlogIds = listOf(1L, 2L)
        val cached = mapOf(
            1L to techBlogSummary(id = 1L, subscriptionCount = 10L, postCount = 3L),
            2L to techBlogSummary(id = 2L, subscriptionCount = 5L, postCount = 1L)
        )

        coEvery { techBlogSummaryCache.mGet(techBlogIds) } returns cached

        val reader = TechBlogStatsReader(
            databaseClient = databaseClient,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = reader.findTechBlogStatsMap(techBlogIds)

        result shouldBe mapOf(
            1L to TechBlogStats(techBlogId = 1L, subscriptionCount = 10L, postCount = 3L),
            2L to TechBlogStats(techBlogId = 2L, subscriptionCount = 5L, postCount = 1L)
        )
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
        coVerify(exactly = 0) { techBlogSummaryCache.mSet(any()) }
    }

    @Test
    fun `캐시 미스가 있으면 조회 후 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val techBlogIds = listOf(1L, 2L, 3L)
        val dbSummaries = listOf(
            techBlogSummary(id = 1L, subscriptionCount = 10L, postCount = 3L),
            techBlogSummary(id = 3L, subscriptionCount = 7L, postCount = 2L)
        )
        val dbMap = dbSummaries.associateBy { it.id }

        coEvery { techBlogSummaryCache.mGet(techBlogIds) } returns mapOf(
            1L to null,
            2L to null,
            3L to null
        )
        every { techBlogSummaryCache.key(1L) } returns "TECHBLOG:SUMMARY:1"
        every { techBlogSummaryCache.key(3L) } returns "TECHBLOG:SUMMARY:3"
        coEvery { techBlogSummaryCache.mSet(dbMap) } returns Unit

        mockSummaryList(databaseClient = databaseClient, bindIds = techBlogIds, summaries = dbSummaries)

        val reader = TechBlogStatsReader(
            databaseClient = databaseClient,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = reader.findTechBlogStatsMap(techBlogIds)
        advanceUntilIdle()

        result shouldBe mapOf(
            1L to TechBlogStats(techBlogId = 1L, subscriptionCount = 10L, postCount = 3L),
            3L to TechBlogStats(techBlogId = 3L, subscriptionCount = 7L, postCount = 2L)
        )
        coVerify(exactly = 1) { techBlogSummaryCache.mSet(dbMap) }
    }

    @Test
    fun `캐시된 단건 통계가 있으면 캐시에서 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val cached = techBlogSummary(id = 1L, subscriptionCount = 10L, postCount = 3L)
        coEvery { techBlogSummaryCache.get(1L) } returns cached

        val reader = TechBlogStatsReader(
            databaseClient = databaseClient,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = reader.findById(1L)

        result shouldBe TechBlogStats(techBlogId = 1L, subscriptionCount = 10L, postCount = 3L)
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
    }

    @Test
    fun `캐시된 단건 통계가 없으면 조회 후 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val summary = techBlogSummary(id = 1L, subscriptionCount = 10L, postCount = 3L)

        coEvery { techBlogSummaryCache.get(1L) } returns null
        every { techBlogSummaryCache.key(1L) } returns "TECHBLOG:SUMMARY:1"
        coEvery { techBlogSummaryCache.set(summary) } returns Unit

        mockSummaryOne(databaseClient = databaseClient, summary = summary)

        val reader = TechBlogStatsReader(
            databaseClient = databaseClient,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = reader.findById(1L)
        advanceUntilIdle()

        result shouldBe TechBlogStats(techBlogId = 1L, subscriptionCount = 10L, postCount = 3L)
        coVerify(exactly = 1) { techBlogSummaryCache.set(summary) }
    }

    private fun techBlogSummary(
        id: Long,
        subscriptionCount: Long,
        postCount: Long,
    ) = TechBlogSummary(
        id = id,
        title = "blog-$id",
        icon = "icon-$id",
        blogUrl = "https://blog.example.com/$id",
        key = "blog-key-$id",
        subscriptionCount = subscriptionCount,
        postCount = postCount,
        subscribed = false,
        notificationEnabled = false
    )

    private fun mockSummaryList(
        databaseClient: DatabaseClient,
        bindIds: List<Long>,
        summaries: List<TechBlogSummary>,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<TechBlogSummary>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        bindIds.forEachIndexed { index, id ->
            every { executeSpec.bind("id$index", id) } returns executeSpec
        }
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, TechBlogSummary>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.all() } returns Flux.fromIterable(summaries)
    }

    private fun mockSummaryOne(
        databaseClient: DatabaseClient,
        summary: TechBlogSummary?,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<TechBlogSummary>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        every { executeSpec.bind("id", any<Long>()) } returns executeSpec
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, TechBlogSummary>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.one() } returns if (summary == null) {
            Mono.empty()
        } else {
            Mono.just(summary)
        }
    }
}
