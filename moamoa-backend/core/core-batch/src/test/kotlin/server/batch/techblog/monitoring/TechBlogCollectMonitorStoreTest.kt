package server.batch.techblog.monitoring

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.batch.techblog.dto.TechBlogKey
import server.cache.CacheMemory
import test.UnitTest

class TechBlogCollectMonitorStoreTest : UnitTest() {

    @Test
    fun `run id가 바뀌면 스냅샷을 초기화한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val sut = TechBlogCollectMonitorStore(cacheMemory)
        var current: TechBlogCollectMonitorSnapshot? = null

        coEvery { cacheMemory.get(TechBlogCollectMonitorStore.KEY, TechBlogCollectMonitorSnapshot::class.java) } answers { current }
        coEvery { cacheMemory.set(TechBlogCollectMonitorStore.KEY, any<Any>(), null) } answers {
            current = secondArg<Any>() as TechBlogCollectMonitorSnapshot
            Unit
        }

        sut.recordFetchSuccess(1000L, TechBlogKey(1L, "a", "A"), fetchedPostCount = 3)
        sut.accumulateAddedCount(1000L, mapOf(1L to 2))

        val first = sut.getLatest()!!
        first.collectRunId shouldBe 1000L
        first.totals.sourceCount shouldBe 1
        first.totals.fetchedPostCount shouldBe 3
        first.totals.addedPostCount shouldBe 2

        sut.recordFetchSuccess(2000L, TechBlogKey(2L, "b", "B"), fetchedPostCount = 1)

        val second = sut.getLatest()!!
        second.collectRunId shouldBe 2000L
        second.totals.sourceCount shouldBe 1
        second.totals.fetchedPostCount shouldBe 1
        second.totals.addedPostCount shouldBe 0
        second.sources.first().techBlogId shouldBe 2L
    }

    @Test
    fun `실패 메시지는 200자로 잘라 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val sut = TechBlogCollectMonitorStore(cacheMemory)
        var current: TechBlogCollectMonitorSnapshot? = null

        coEvery { cacheMemory.get(TechBlogCollectMonitorStore.KEY, TechBlogCollectMonitorSnapshot::class.java) } answers { current }
        coEvery { cacheMemory.set(TechBlogCollectMonitorStore.KEY, any<Any>(), null) } answers {
            current = secondArg<Any>() as TechBlogCollectMonitorSnapshot
            Unit
        }

        val longMessage = "x".repeat(250)
        sut.recordFetchFailure(3000L, TechBlogKey(3L, "c", "C"), IllegalStateException(longMessage))

        val snapshot = sut.getLatest()!!
        val errorMessage = snapshot.sources.first().errorMessage ?: ""
        errorMessage.length shouldBe 200
    }

    @Test
    fun `added count를 누적 합산한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val sut = TechBlogCollectMonitorStore(cacheMemory)
        var current: TechBlogCollectMonitorSnapshot? = null

        coEvery { cacheMemory.get(TechBlogCollectMonitorStore.KEY, TechBlogCollectMonitorSnapshot::class.java) } answers { current }
        coEvery { cacheMemory.set(TechBlogCollectMonitorStore.KEY, any<Any>(), null) } answers {
            current = secondArg<Any>() as TechBlogCollectMonitorSnapshot
            Unit
        }

        val runId = 4000L
        sut.recordFetchSuccess(runId, TechBlogKey(10L, "x", "X"), fetchedPostCount = 4)
        sut.accumulateAddedCount(runId, mapOf(10L to 2))
        sut.accumulateAddedCount(runId, mapOf(10L to 3))

        val snapshot = sut.getLatest()!!
        snapshot.sources.first().addedPostCount shouldBe 5
        snapshot.totals.addedPostCount shouldBe 5
    }
}
