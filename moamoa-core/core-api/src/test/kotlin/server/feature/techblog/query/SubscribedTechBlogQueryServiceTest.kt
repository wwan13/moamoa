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
import server.feature.member.command.domain.MemberRole
import server.infra.cache.TechBlogSummaryCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport
import test.UnitTest
import java.util.function.BiFunction

class SubscribedTechBlogQueryServiceTest : UnitTest() {
    private val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

    init {
        every { warmupCoordinator.launchIfAbsent(any(), any()) } answers {
            runBlocking { secondArg<suspend () -> Unit>().invoke() }
        }
    }
    @Test
    fun `구독중인 기술 블로그가 없으면 빈 결과를 반환한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)

        coEvery { subscribedTechBlogReader.findAllSubscribedList(1L) } returns emptyList()

        val service = SubscribedTechBlogQueryService(
            databaseClient = databaseClient,
            subscribedTechBlogReader = subscribedTechBlogReader,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findSubscribingTechBlogs(passport)

        result.meta shouldBe TechBlogListMeta(totalCount = 0L)
        result.techBlogs shouldBe emptyList()
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
    }

    @Test
    fun `캐시된 요약 정보가 있으면 캐시에서 병합한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)
        val subscriptions = listOf(
            TechBlogSubscriptionInfo(techBlogId = 2L, subscribed = true, notificationEnabled = false),
            TechBlogSubscriptionInfo(techBlogId = 1L, subscribed = true, notificationEnabled = true)
        )
        val cachedMap = mapOf(
            1L to techBlogSummary(id = 1L, title = "alpha"),
            2L to techBlogSummary(id = 2L, title = "beta")
        )

        coEvery { subscribedTechBlogReader.findAllSubscribedList(1L) } returns subscriptions
        coEvery { techBlogSummaryCache.mGet(listOf(2L, 1L)) } returns cachedMap

        val service = SubscribedTechBlogQueryService(
            databaseClient = databaseClient,
            subscribedTechBlogReader = subscribedTechBlogReader,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findSubscribingTechBlogs(passport)

        result.meta shouldBe TechBlogListMeta(totalCount = 2L)
        result.techBlogs.map { it.id } shouldBe listOf(1L, 2L)
        result.techBlogs[0].notificationEnabled shouldBe true
        result.techBlogs[1].notificationEnabled shouldBe false
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
    }

    @Test
    fun `캐시 미스가 있으면 DB에서 조회하고 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()
        val techBlogSummaryCache = mockk<TechBlogSummaryCache>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)
        val subscriptions = listOf(
            TechBlogSubscriptionInfo(techBlogId = 2L, subscribed = true, notificationEnabled = false),
            TechBlogSubscriptionInfo(techBlogId = 1L, subscribed = true, notificationEnabled = true)
        )
        val cachedMap = mapOf(
            1L to techBlogSummary(id = 1L, title = "alpha"),
            2L to null
        )
        val dbSummaries = listOf(
            techBlogSummary(id = 2L, title = "beta")
        )
        val dbMap = dbSummaries.associateBy { it.id }

        coEvery { subscribedTechBlogReader.findAllSubscribedList(1L) } returns subscriptions
        coEvery { techBlogSummaryCache.mGet(listOf(2L, 1L)) } returns cachedMap
        every { techBlogSummaryCache.key(2L) } returns "TECHBLOG:SUMMARY:2"
        coEvery { techBlogSummaryCache.mSet(dbMap) } returns Unit

        mockSummaryList(databaseClient = databaseClient, summaries = dbSummaries)

        val service = SubscribedTechBlogQueryService(
            databaseClient = databaseClient,
            subscribedTechBlogReader = subscribedTechBlogReader,
            techBlogSummaryCache = techBlogSummaryCache,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findSubscribingTechBlogs(passport)
        advanceUntilIdle()

        result.meta shouldBe TechBlogListMeta(totalCount = 2L)
        result.techBlogs.map { it.id } shouldBe listOf(1L, 2L)
        coVerify(exactly = 1) { techBlogSummaryCache.mSet(dbMap) }
    }

    private fun techBlogSummary(
        id: Long,
        title: String,
    ) = TechBlogSummary(
        id = id,
        title = title,
        icon = "icon-$id",
        blogUrl = "https://blog.example.com/$id",
        key = "blog-key-$id",
        subscriptionCount = 0L,
        postCount = 0L,
        subscribed = false,
        notificationEnabled = false
    )

    private fun mockSummaryList(
        databaseClient: DatabaseClient,
        summaries: List<TechBlogSummary>,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<TechBlogSummary>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        summaries.forEachIndexed { index, summary ->
            every { executeSpec.bind("id$index", summary.id) } returns executeSpec
        }
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, TechBlogSummary>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.all() } returns Flux.fromIterable(summaries)
    }
}
