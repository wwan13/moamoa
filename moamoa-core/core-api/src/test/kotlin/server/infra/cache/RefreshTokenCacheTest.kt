package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import test.UnitTest

class RefreshTokenCacheTest : UnitTest() {
    @Test
    fun `리프레시 토큰을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = RefreshTokenCache(cacheMemory)
        val key = "REFRESH_TOKEN:5"

        coEvery { cacheMemory.set(key, "token", 30_000L) } just Runs

        cache.set(5L, "token", 30_000L)

        coVerify(exactly = 1) { cacheMemory.set(key, "token", 30_000L) }
    }

    @Test
    fun `리프레시 토큰을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = RefreshTokenCache(cacheMemory)
        val key = "REFRESH_TOKEN:5"

        coEvery { cacheMemory.get(key, any<TypeReference<String>>()) } returns "token"

        val result = cache.get(5L)

        result shouldBe "token"
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<String>>()) }
    }

    @Test
    fun `리프레시 토큰을 삭제한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = RefreshTokenCache(cacheMemory)
        val key = "REFRESH_TOKEN:5"

        coEvery { cacheMemory.evict(key) } just Runs

        cache.evict(5L)

        coVerify(exactly = 1) { cacheMemory.evict(key) }
    }
}
