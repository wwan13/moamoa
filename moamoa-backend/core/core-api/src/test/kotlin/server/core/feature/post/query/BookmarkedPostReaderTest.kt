package server.core.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import server.core.feature.post.query.BookmarkedPostReader
import server.core.feature.post.infra.BookmarkedAllPostIdSetCache
import server.core.infra.cache.WarmupCoordinator
import test.UnitTest

class BookmarkedPostReaderTest : UnitTest() {
    @Test
    fun `캐시된 북마크가 있으면 캐시에서 필터링한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val bookmarkedAllPostIdSetCache = mockk<BookmarkedAllPostIdSetCache>()

        every { bookmarkedAllPostIdSetCache.get(1L) } returns setOf(1L, 3L)

        val reader = BookmarkedPostReader(
            entityManager = entityManager,
            bookmarkedAllPostIdSetCache = bookmarkedAllPostIdSetCache,
            warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)
        )

        val result = reader.findBookmarkedPostIdSet(1L, listOf(1L, 2L, 3L))

        result shouldBe setOf(1L, 3L)
    }
}
