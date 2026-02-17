package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.cache.CacheMemory
import server.shared.cache.get
import server.shared.cache.mgetAs
import server.feature.techblog.query.TechBlogSummary
import server.fixture.createTechBlogSummary
import test.UnitTest

class TechBlogSummaryCacheTest : UnitTest() {
    @Test
    fun `기술 블로그 요약 정보를 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)
        val key = "TECHBLOG:SUMMARY:10"
        val expected = createTechBlogSummary(id = 10L)

        coEvery { cacheMemory.get(key, any<TypeReference<TechBlogSummary>>()) } returns expected

        val result = cache.get(10L)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<TechBlogSummary>>()) }
    }

    @Test
    fun `id 목록이 비어있으면 빈 맵을 반환한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)

        val result = cache.mGet(emptyList())

        result shouldBe emptyMap()
        coVerify(exactly = 0) { cacheMemory.mgetAs(any(), any<TypeReference<TechBlogSummary>>()) }
    }

    @Test
    fun `id 목록으로 기술 블로그 요약 정보를 일괄 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)
        val key1 = "TECHBLOG:SUMMARY:1"
        val key2 = "TECHBLOG:SUMMARY:2"
        val summary1 = createTechBlogSummary(id = 1L)

        coEvery { cacheMemory.mgetAs(setOf(key1, key2), any<TypeReference<TechBlogSummary>>()) } returns mapOf(
            key1 to summary1,
            key2 to null
        )

        val result = cache.mGet(listOf(1L, 1L, 2L))

        result shouldBe mapOf(1L to summary1, 2L to null)
        coVerify(exactly = 1) { cacheMemory.mgetAs(setOf(key1, key2), any<TypeReference<TechBlogSummary>>()) }
    }

    @Test
    fun `구독 상태를 정규화해서 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)
        val summary = createTechBlogSummary(id = 1L, subscribed = true, notificationEnabled = true)
        val key = "TECHBLOG:SUMMARY:1"
        val expected = summary.copy(subscribed = false, notificationEnabled = false)

        coEvery { cacheMemory.set(key, expected, 60_000L) } just Runs

        cache.set(summary)

        coVerify(exactly = 1) { cacheMemory.set(key, expected, 60_000L) }
    }

    @Test
    fun `요약 정보가 비어있으면 일괄 저장을 건너뛴다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)

        cache.mSet(emptyMap())

        coVerify(exactly = 0) { cacheMemory.mset(any(), any()) }
    }

    @Test
    fun `기술 블로그 요약 정보를 정규화해서 일괄 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)
        val summary1 = createTechBlogSummary(id = 1L, subscribed = true, notificationEnabled = true)
        val summary2 = createTechBlogSummary(id = 2L, subscribed = true, notificationEnabled = false)
        val payload = mapOf(
            "TECHBLOG:SUMMARY:1" to summary1.copy(subscribed = false, notificationEnabled = false),
            "TECHBLOG:SUMMARY:2" to summary2.copy(subscribed = false, notificationEnabled = false)
        )

        coEvery { cacheMemory.mset(payload, 60_000L) } just Runs

        cache.mSet(mapOf(1L to summary1, 2L to summary2))

        coVerify(exactly = 1) { cacheMemory.mset(payload, 60_000L) }
    }

    @Test
    fun `기술 블로그 요약 정보를 삭제한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogSummaryCache(cacheMemory)
        val key = "TECHBLOG:SUMMARY:10"

        coEvery { cacheMemory.evict(key) } just Runs

        cache.evict(10L)

        coVerify(exactly = 1) { cacheMemory.evict(key) }
    }
}
