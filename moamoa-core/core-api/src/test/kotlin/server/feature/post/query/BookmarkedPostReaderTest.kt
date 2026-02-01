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
import server.infra.cache.BookmarkedAllPostIdSetCache
import test.UnitTest
import java.util.function.BiFunction

class BookmarkedPostReaderTest : UnitTest() {
    @Test
    fun `캐시된 북마크가 있으면 캐시에서 필터링한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>()

        val memberId = 1L
        val postIds = listOf(1L, 2L, 3L)

        coEvery { bookmarkedAllPostIdSetCache.get(memberId) } returns setOf(1L, 3L)

        val reader = BookmarkedPostReader(
            databaseClient = databaseClient,
            bookmarkedAllPostIdSetCache = bookmarkedAllPostIdSetCache,
            cacheWarmupScope = this
        )

        val result = reader.findBookmarkedPostIdSet(memberId, postIds)

        result shouldBe setOf(1L, 3L)
        verify(exactly = 0) { databaseClient.sql(any<String>()) }
        coVerify(exactly = 0) { bookmarkedAllPostIdSetCache.set(any(), any()) }
    }

    @Test
    fun `캐시된 북마크가 없으면 조회 후 캐시 워밍업을 수행한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>()

        val memberId = 1L
        val postIds = listOf(10L, 20L, 30L)

        val fetchSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val fetchRowsFetchSpec = mockk<RowsFetchSpec<Long>>()
        val warmupSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val warmupRowsFetchSpec = mockk<RowsFetchSpec<Long>>()

        coEvery { bookmarkedAllPostIdSetCache.get(memberId) } returnsMany listOf(null, null)
        coEvery { bookmarkedAllPostIdSetCache.set(memberId, any()) } returns Unit

        every { databaseClient.sql(any<String>()) } answers {
            val sql = firstArg<String>()
            if (sql.contains("IN (")) fetchSpec else warmupSpec
        }

        every { fetchSpec.bind("memberId", memberId) } returns fetchSpec
        every { fetchSpec.bind("id0", 10L) } returns fetchSpec
        every { fetchSpec.bind("id1", 20L) } returns fetchSpec
        every { fetchSpec.bind("id2", 30L) } returns fetchSpec
        every { fetchSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns fetchRowsFetchSpec
        every { fetchRowsFetchSpec.all() } returns Flux.fromIterable(listOf(10L, 30L))

        every { warmupSpec.bind("memberId", memberId) } returns warmupSpec
        every { warmupSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns warmupRowsFetchSpec
        every { warmupRowsFetchSpec.all() } returns Flux.fromIterable(listOf(10L, 20L, 30L))

        val reader = BookmarkedPostReader(
            databaseClient = databaseClient,
            bookmarkedAllPostIdSetCache = bookmarkedAllPostIdSetCache,
            cacheWarmupScope = this
        )

        val result = reader.findBookmarkedPostIdSet(memberId, postIds)
        advanceUntilIdle()

        result shouldBe setOf(10L, 30L)
        coVerify(exactly = 1) { bookmarkedAllPostIdSetCache.set(memberId, setOf(10L, 20L, 30L)) }
    }
}
