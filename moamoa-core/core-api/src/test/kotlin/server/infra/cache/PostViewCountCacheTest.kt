package server.infra.cache

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import server.set.SetMemory
import test.UnitTest

class PostViewCountCacheTest : UnitTest() {
    @Test
    fun `조회수를 증가시키고 dirty set에 post id를 기록한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val setMemory = mockk<SetMemory>()
        val cache = PostViewCountCache(cacheMemory, setMemory)
        val key = "POST:VIEW_COUNT:10"
        val dirtySetKey = "POST:VIEW_COUNT:DIRTY_SET"

        coEvery { cacheMemory.incr(key) } returns 1L
        coEvery { setMemory.add(dirtySetKey, "10") } returns true

        cache.incr(10L)

        coVerify(exactly = 1) { cacheMemory.incr(key) }
        coVerify(exactly = 1) { setMemory.add(dirtySetKey, "10") }
    }
}
