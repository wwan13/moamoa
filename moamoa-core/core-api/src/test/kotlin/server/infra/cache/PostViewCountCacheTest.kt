package server.infra.cache

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import test.UnitTest

class PostViewCountCacheTest : UnitTest() {
    @Test
    fun `조회수를 증가시킨다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostViewCountCache(cacheMemory)
        val key = "POST:VIEW_COUNT:10"

        coEvery { cacheMemory.incr(key) } returns 1L

        cache.incr(10L)

        coVerify(exactly = 1) { cacheMemory.incr(key) }
    }
}
