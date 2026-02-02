package server.feature.techblog.query

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
import server.infra.cache.TechBlogSubscriptionCache
import test.UnitTest
import java.util.function.BiFunction

class SubscribedTechBlogReaderTest : UnitTest() {
    @Test
    fun `캐시된 구독 목록이 있으면 캐시에서 필터링한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSubscriptionCache = mockk<TechBlogSubscriptionCache>()

        val memberId = 1L
        val techBlogIds = listOf(1L, 2L, 3L)
        val cached = listOf(
            TechBlogSubscriptionInfo(techBlogId = 1L, subscribed = true, notificationEnabled = false),
            TechBlogSubscriptionInfo(techBlogId = 3L, subscribed = true, notificationEnabled = true)
        )

        coEvery { techBlogSubscriptionCache.get(memberId) } returns cached

        val reader = SubscribedTechBlogReader(
            databaseClient = databaseClient,
            techBlogSubscriptionCache = techBlogSubscriptionCache,
            cacheWarmupScope = this
        )

        val result = reader.findSubscribedMap(memberId, techBlogIds)

        result.keys shouldBe setOf(1L, 3L)
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
        coVerify(exactly = 0) { techBlogSubscriptionCache.set(any(), any()) }
    }

    @Test
    fun `캐시된 구독 목록이 없으면 조회 후 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val techBlogSubscriptionCache = mockk<TechBlogSubscriptionCache>()

        val memberId = 1L
        val techBlogIds = listOf(1L, 2L, 3L)
        val dbList = listOf(
            TechBlogSubscriptionInfo(techBlogId = 1L, subscribed = true, notificationEnabled = false),
            TechBlogSubscriptionInfo(techBlogId = 3L, subscribed = true, notificationEnabled = true)
        )

        coEvery { techBlogSubscriptionCache.get(memberId) } returns null
        coEvery { techBlogSubscriptionCache.set(memberId, dbList) } returns Unit

        mockSubscriptionList(databaseClient = databaseClient, memberId = memberId, list = dbList)

        val reader = SubscribedTechBlogReader(
            databaseClient = databaseClient,
            techBlogSubscriptionCache = techBlogSubscriptionCache,
            cacheWarmupScope = this
        )

        val result = reader.findSubscribedMap(memberId, techBlogIds)
        advanceUntilIdle()

        result.keys shouldBe setOf(1L, 3L)
        coVerify(exactly = 1) { techBlogSubscriptionCache.set(memberId, dbList) }
    }

    private fun mockSubscriptionList(
        databaseClient: DatabaseClient,
        memberId: Long,
        list: List<TechBlogSubscriptionInfo>,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<TechBlogSubscriptionInfo>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        every { executeSpec.bind("memberId", memberId) } returns executeSpec
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, TechBlogSubscriptionInfo>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.all() } returns Flux.fromIterable(list)
    }
}
