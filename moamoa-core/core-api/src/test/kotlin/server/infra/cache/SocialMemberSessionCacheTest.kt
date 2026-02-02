package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import test.UnitTest

class SocialMemberSessionCacheTest : UnitTest() {
    @Test
    fun `소셜 가입 세션을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SocialMemberSessionCache(cacheMemory)
        val key = "SOCIAL:SIGNUP:token"

        coEvery { cacheMemory.set(key, 10L, 60_000L) } just Runs

        cache.set("token", 10L)

        coVerify(exactly = 1) { cacheMemory.set(key, 10L, 60_000L) }
    }

    @Test
    fun `소셜 가입 세션을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SocialMemberSessionCache(cacheMemory)
        val key = "SOCIAL:SIGNUP:token"

        coEvery { cacheMemory.get(key, any<TypeReference<Long>>()) } returns 10L

        val result = cache.get("token")

        result shouldBe 10L
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<Long>>()) }
    }

    @Test
    fun `소셜 가입 세션을 삭제한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SocialMemberSessionCache(cacheMemory)
        val key = "SOCIAL:SIGNUP:token"

        coEvery { cacheMemory.evict(key) } just Runs

        cache.evict("token")

        coVerify(exactly = 1) { cacheMemory.evict(key) }
    }
}
