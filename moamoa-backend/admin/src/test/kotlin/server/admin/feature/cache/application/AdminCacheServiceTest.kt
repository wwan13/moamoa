package server.admin.feature.cache.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import test.UnitTest

class AdminCacheServiceTest : UnitTest() {

    @Test
    fun `core-api cache 목록을 eviction strategy와 함께 조회한다`() {
        val service = AdminCacheService(mockk(relaxed = true))

        val result = service.findAll()

        result.shouldHaveSize(9)
        result.first { it.key == "techblog-list" }.evictionStrategy shouldBe "exact_key"
        result.first { it.key == "subscribed-post-list" }.evictionStrategy shouldBe "versioned_prefix"
    }

    @Test
    fun `prefix 기반 캐시를 evict 한다`() {
        val cacheMemory = mockk<CacheMemory>(relaxed = true)
        val service = AdminCacheService(cacheMemory)

        val result = service.evict("techblog-summary")

        result.key shouldBe "techblog-summary"
        verify(exactly = 1) { cacheMemory.evictByPrefix("TECHBLOG:SUMMARY:") }
    }

    @Test
    fun `exact key 캐시를 evict 한다`() {
        val cacheMemory = mockk<CacheMemory>(relaxed = true)
        val service = AdminCacheService(cacheMemory)

        val result = service.evict("techblog-list")

        result.evictionStrategy shouldBe "exact_key"
        verify(exactly = 1) { cacheMemory.evict("TECHBLOG:BASE:LIST") }
    }

    @Test
    fun `존재하지 않는 캐시는 evict 할 수 없다`() {
        val service = AdminCacheService(mockk(relaxed = true))

        val error = shouldThrow<NoSuchElementException> {
            service.evict("refresh-token")
        }

        error.message shouldBe "지원하지 않는 캐시입니다: refresh-token"
    }
}
