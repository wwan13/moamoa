package server.batch.post.updatepostviewcount.reader

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import server.batch.post.updatepostviewcount.dto.PostViewCount
import server.cache.CacheMemory
import server.set.SetMemory
import test.UnitTest

class UpdatePostViewCountReaderTest : UnitTest() {

    @Test
    fun `dirty set 멤버를 mget으로 조회해 writer 아이템으로 반환한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory)

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returns setOf(
            "1",
            "2",
            "bad"
        )
        coEvery { cacheMemory.mget(any()) } returns mapOf(
            "POST:VIEW_COUNT:1" to "3",
            "POST:VIEW_COUNT:2" to "0"
        )

        runBlocking {
            sut.loadItems() shouldBe listOf(
                PostViewCount(postId = 1L, delta = 3L, cacheKey = "POST:VIEW_COUNT:1")
            )
        }
    }

    @Test
    fun `dirty set이 비어 있으면 null을 반환한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory)

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returns emptySet()

        runBlocking {
            sut.loadItems() shouldBe emptyList()
        }
    }

    @Test
    fun `호출 시점마다 목록을 새로 로드한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory)

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returnsMany listOf(
            setOf("10"),
            setOf("20")
        )
        coEvery { cacheMemory.mget(any()) } returnsMany listOf(
            mapOf("POST:VIEW_COUNT:10" to "4"),
            mapOf("POST:VIEW_COUNT:20" to "6")
        )

        runBlocking {
            sut.loadItems() shouldBe listOf(
                PostViewCount(postId = 10L, delta = 4L, cacheKey = "POST:VIEW_COUNT:10")
            )
            sut.loadItems() shouldBe listOf(
                PostViewCount(postId = 20L, delta = 6L, cacheKey = "POST:VIEW_COUNT:20")
            )
        }
    }
}
