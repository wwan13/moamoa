package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import server.fixture.createTechBlogSummary
import test.UnitTest

class TechBlogListCacheTest : UnitTest() {
    @Test
    fun `기술 블로그 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogListCache(cacheMemory)
        val key = "TECHBLOG:BASE:LIST"
        val expected = listOf(createTechBlogSummary())

        coEvery { cacheMemory.get(key, any<TypeReference<List<server.feature.techblog.query.TechBlogSummary>>>()) } returns expected

        val result = cache.get()

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<List<server.feature.techblog.query.TechBlogSummary>>>()) }
    }

    @Test
    fun `기술 블로그 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogListCache(cacheMemory)
        val key = "TECHBLOG:BASE:LIST"
        val list = listOf(createTechBlogSummary())

        coEvery { cacheMemory.set(key, list, 1_800_000L) } just Runs

        cache.set(list)

        coVerify(exactly = 1) { cacheMemory.set(key, list, 1_800_000L) }
    }

    @Test
    fun `기술 블로그 목록을 삭제한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogListCache(cacheMemory)
        val key = "TECHBLOG:BASE:LIST"

        coEvery { cacheMemory.evict(key) } just Runs

        cache.evict()

        coVerify(exactly = 1) { cacheMemory.evict(key) }
    }
}
