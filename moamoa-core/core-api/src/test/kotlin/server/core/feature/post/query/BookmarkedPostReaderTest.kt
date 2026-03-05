package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.core.feature.post.query.BookmarkedPostReader
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class BookmarkedPostReaderTest : UnitTest() {
    @Test
    fun `캐시된 북마크가 있으면 캐시에서 필터링한다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>()

        every { bookmarkedAllPostIdSetCache.get(1L) } returns setOf(1L, 3L)

        val reader = BookmarkedPostReader(
            jdbc = jdbc,
            bookmarkedAllPostIdSetCache = bookmarkedAllPostIdSetCache,
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = reader.findBookmarkedPostIdSet(1L, listOf(1L, 2L, 3L))

        result shouldBe setOf(1L, 3L)
    }
}
