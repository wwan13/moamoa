package server.core.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.techblog.query.SubscriptionInfo
import server.core.infra.cache.SubscriptionCache
import server.cache.CacheMemory
import server.core.fixture.createSubscriptionInfo
import test.UnitTest

class SubscriptionCacheTest : UnitTest() {
    @Test
    fun `버전이 없으면 기본 버전으로 구독 정보를 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscriptionCache(cacheMemory)
        val memberId = 4L
        val versionKey = "TECHBLOG:SUBSCRIPTION:ALL:$memberId:VER"
        val valueKey = "TECHBLOG:SUBSCRIPTION:ALL:$memberId:V:1"
        val expected = listOf(createSubscriptionInfo())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns null
        coEvery { cacheMemory.get(valueKey, any<TypeReference<List<SubscriptionInfo>>>()) } returns expected

        val result = cache.get(memberId)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.get(valueKey, any<TypeReference<List<SubscriptionInfo>>>()) }
    }

    @Test
    fun `버전 키를 사용해 구독 정보를 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscriptionCache(cacheMemory)
        val memberId = 4L
        val versionKey = "TECHBLOG:SUBSCRIPTION:ALL:$memberId:VER"
        val valueKey = "TECHBLOG:SUBSCRIPTION:ALL:$memberId:V:2"
        val value = listOf(createSubscriptionInfo())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns 2L
        coEvery { cacheMemory.set(valueKey, value, 60_000L) } just Runs

        cache.set(memberId, value)

        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.set(valueKey, value, 60_000L) }
    }

    @Test
    fun `버전을 증가시켜 구독 정보 캐시를 무효화한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscriptionCache(cacheMemory)
        val memberId = 4L
        val versionKey = "TECHBLOG:SUBSCRIPTION:ALL:$memberId:VER"

        coEvery { cacheMemory.incr(versionKey) } returns 2L

        cache.evictAll(memberId)

        coVerify(exactly = 1) { cacheMemory.incr(versionKey) }
    }
}
