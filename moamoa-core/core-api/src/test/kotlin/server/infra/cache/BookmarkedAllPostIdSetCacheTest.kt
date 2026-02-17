package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.cache.CacheMemory
import server.shared.cache.get
import test.UnitTest

class BookmarkedAllPostIdSetCacheTest : UnitTest() {
    @Test
    fun `버전이 없으면 기본 버전으로 캐시를 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedAllPostIdSetCache(cacheMemory)
        val memberId = 10L
        val versionKey = "POST:BOOKMARKED:ALL:$memberId:VER"
        val valueKey = "POST:BOOKMARKED:ALL:$memberId:V:1"
        val expected = setOf(1L, 2L)

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns null
        coEvery { cacheMemory.get(valueKey, any<TypeReference<Set<Long>>>()) } returns expected

        val result = cache.get(memberId)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.get(valueKey, any<TypeReference<Set<Long>>>()) }
    }

    @Test
    fun `버전 키를 사용해 캐시를 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedAllPostIdSetCache(cacheMemory)
        val memberId = 10L
        val versionKey = "POST:BOOKMARKED:ALL:$memberId:VER"
        val valueKey = "POST:BOOKMARKED:ALL:$memberId:V:3"
        val postIds = setOf(3L, 4L)

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns 3L
        coEvery { cacheMemory.set(valueKey, postIds, 60_000L) } just Runs

        cache.set(memberId, postIds)

        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.set(valueKey, postIds, 60_000L) }
    }

    @Test
    fun `전체 캐시 무효화를 위해 버전을 증가시킨다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedAllPostIdSetCache(cacheMemory)
        val memberId = 10L
        val versionKey = "POST:BOOKMARKED:ALL:$memberId:VER"

        coEvery { cacheMemory.incr(versionKey) } returns 2L

        cache.evictAll(memberId)

        coVerify(exactly = 1) { cacheMemory.incr(versionKey) }
    }
}
