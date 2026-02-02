package server.feature.techblog.query

import io.kotest.assertions.throwables.shouldThrow
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
import reactor.core.publisher.Mono
import server.feature.member.command.domain.MemberRole
import server.infra.cache.TechBlogListCache
import server.security.Passport
import test.UnitTest
import java.util.function.BiFunction

class TechBlogQueryServiceTest : UnitTest() {
    @Test
    fun `캐시된 기술 블로그 목록이 있으면 캐시에서 조회하고 결과를 병합한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)
        val conditions = TechBlogQueryConditions(query = null)

        val baseList = listOf(
            techBlogSummary(id = 1L, title = "blog-a"),
            techBlogSummary(id = 2L, title = "blog-b")
        )
        val statsMap = mapOf(
            1L to TechBlogStats(techBlogId = 1L, subscriptionCount = 3L, postCount = 5L)
        )
        val subscribedMap = mapOf(
            2L to TechBlogSubscriptionInfo(
                techBlogId = 2L,
                subscribed = true,
                notificationEnabled = true
            )
        )

        coEvery { techBlogListCache.get() } returns baseList
        coEvery { techBlogStatsReader.findTechBlogStatsMap(listOf(1L, 2L)) } returns statsMap
        coEvery { subscribedTechBlogReader.findSubscribedMap(1L, listOf(1L, 2L)) } returns subscribedMap

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        val result = service.findAll(passport, conditions)

        result.meta shouldBe TechBlogListMeta(totalCount = 2L)
        result.techBlogs.map { it.id } shouldBe listOf(1L, 2L)
        result.techBlogs[0].subscriptionCount shouldBe 3L
        result.techBlogs[0].postCount shouldBe 5L
        result.techBlogs[0].subscribed shouldBe false
        result.techBlogs[1].subscriptionCount shouldBe 0L
        result.techBlogs[1].postCount shouldBe 0L
        result.techBlogs[1].subscribed shouldBe true
        result.techBlogs[1].notificationEnabled shouldBe true
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
    }

    @Test
    fun `캐시된 기술 블로그 목록이 없으면 데이터베이스에서 조회하고 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)
        val conditions = TechBlogQueryConditions(query = null)

        val baseList = listOf(
            techBlogSummary(id = 1L, title = "blog-a"),
            techBlogSummary(id = 2L, title = "blog-b")
        )

        coEvery { techBlogListCache.get() } returns null
        coEvery { techBlogListCache.set(baseList) } returns Unit
        coEvery { techBlogStatsReader.findTechBlogStatsMap(listOf(1L, 2L)) } returns emptyMap()
        coEvery { subscribedTechBlogReader.findSubscribedMap(1L, listOf(1L, 2L)) } returns emptyMap()

        mockList(databaseClient = databaseClient, baseList = baseList)

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        val result = service.findAll(passport, conditions)
        advanceUntilIdle()

        result.meta shouldBe TechBlogListMeta(totalCount = 2L)
        result.techBlogs.map { it.id } shouldBe listOf(1L, 2L)
        coVerify(exactly = 1) { techBlogListCache.set(baseList) }
    }

    @Test
    fun `검색 키워드가 있으면 캐시를 사용하지 않고 키워드 조건으로 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val passport = Passport(memberId = 1L, role = MemberRole.USER)
        val conditions = TechBlogQueryConditions(query = "kotlin")

        val baseList = listOf(
            techBlogSummary(id = 1L, title = "blog-a")
        )

        coEvery { techBlogStatsReader.findTechBlogStatsMap(listOf(1L)) } returns emptyMap()
        coEvery { subscribedTechBlogReader.findSubscribedMap(1L, listOf(1L)) } returns emptyMap()

        mockList(databaseClient = databaseClient, baseList = baseList, query = "kotlin")

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        service.findAll(passport, conditions)

        coVerify(exactly = 0) { techBlogListCache.get() }
        coVerify(exactly = 0) { techBlogListCache.set(any()) }
    }

    @Test
    fun `passport가 null이면 구독 정보를 조회하지 않는다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val conditions = TechBlogQueryConditions(query = null)

        val baseList = listOf(
            techBlogSummary(id = 1L, title = "blog-a")
        )

        coEvery { techBlogListCache.get() } returns baseList
        coEvery { techBlogStatsReader.findTechBlogStatsMap(listOf(1L)) } returns emptyMap()

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        val result = service.findAll(passport = null, conditions = conditions)

        result.techBlogs[0].subscribed shouldBe false
        coVerify(exactly = 0) { subscribedTechBlogReader.findSubscribedMap(any(), any()) }
    }

    @Test
    fun `기술 블로그가 존재하지 않으면 예외가 발생한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        mockFindById(databaseClient = databaseClient, summary = null)

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        shouldThrow<IllegalStateException> {
            service.findById(passport = null, techBlogId = 1L)
        }
    }

    @Test
    fun `passport가 null이면 구독 정보 없이 기술 블로그를 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val base = techBlogSummary(id = 1L, title = "blog-a")
        mockFindById(databaseClient = databaseClient, summary = base)
        coEvery { techBlogStatsReader.findById(1L) } returns TechBlogStats(
            techBlogId = 1L,
            subscriptionCount = 3L,
            postCount = 5L
        )

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        val result = service.findById(passport = null, techBlogId = 1L)

        result.subscriptionCount shouldBe 3L
        result.postCount shouldBe 5L
        result.subscribed shouldBe false
        coVerify(exactly = 0) { subscribedTechBlogReader.findById(any(), any()) }
    }

    @Test
    fun `passport가 있으면 구독 정보를 합친다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogListCache = mockk<TechBlogListCache>()
        val techBlogStatsReader = mockk<TechBlogStatsReader>()
        val subscribedTechBlogReader = mockk<SubscribedTechBlogReader>()

        val base = techBlogSummary(id = 1L, title = "blog-a")
        mockFindById(databaseClient = databaseClient, summary = base)
        coEvery { techBlogStatsReader.findById(1L) } returns TechBlogStats(
            techBlogId = 1L,
            subscriptionCount = 3L,
            postCount = 5L
        )
        coEvery { subscribedTechBlogReader.findById(1L, 1L) } returns TechBlogSubscriptionInfo(
            techBlogId = 1L,
            subscribed = true,
            notificationEnabled = true
        )

        val service = TechBlogQueryService(
            databaseClient = databaseClient,
            techBlogListCache = techBlogListCache,
            techBlogStatsReader = techBlogStatsReader,
            subscribedTechBlogReader = subscribedTechBlogReader,
            cacheWarmupScope = this
        )

        val result = service.findById(
            passport = Passport(memberId = 1L, role = MemberRole.USER),
            techBlogId = 1L
        )

        result.subscriptionCount shouldBe 3L
        result.postCount shouldBe 5L
        result.subscribed shouldBe true
        result.notificationEnabled shouldBe true
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

    private fun mockList(
        databaseClient: DatabaseClient,
        baseList: List<TechBlogSummary>,
        query: String? = null,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<TechBlogSummary>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        if (query != null) {
            every { executeSpec.bind("keyword", "%$query%") } returns executeSpec
        }
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, TechBlogSummary>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.all() } returns Flux.fromIterable(baseList)
    }

    private fun mockFindById(
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
