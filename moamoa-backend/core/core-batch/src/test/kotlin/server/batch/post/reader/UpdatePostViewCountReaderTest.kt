package server.batch.post.reader

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ExecutionContext
import server.batch.post.dto.PostViewCount
import server.cache.CacheMemory
import server.set.SetMemory
import test.UnitTest

class UpdatePostViewCountReaderTest : UnitTest() {

    @Test
    fun `dirty set 멤버를 mget으로 조회해 writer 아이템으로 반환한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory).build()

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returns setOf(
            "1",
            "2",
            "bad"
        )
        coEvery { cacheMemory.mget(any()) } returns mapOf(
            "POST:VIEW_COUNT:1" to "3",
            "POST:VIEW_COUNT:2" to "0"
        )

        sut.open(ExecutionContext())

        sut.read() shouldBe PostViewCount(postId = 1L, delta = 3L, cacheKey = "POST:VIEW_COUNT:1")
        sut.read() shouldBe null
    }

    @Test
    fun `dirty set이 비어 있으면 null을 반환한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory).build()

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returns emptySet()

        sut.open(ExecutionContext())

        sut.read() shouldBe null
    }

    @Test
    fun `step open 시점마다 목록을 새로 로드한다`() {
        val setMemory = mockk<SetMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = UpdatePostViewCountReader(setMemory, cacheMemory).build()

        coEvery { setMemory.members("POST:VIEW_COUNT:DIRTY_SET") } returnsMany listOf(
            setOf("10"),
            setOf("20")
        )
        coEvery { cacheMemory.mget(any()) } returnsMany listOf(
            mapOf("POST:VIEW_COUNT:10" to "4"),
            mapOf("POST:VIEW_COUNT:20" to "6")
        )

        sut.open(ExecutionContext())
        sut.read() shouldBe PostViewCount(postId = 10L, delta = 4L, cacheKey = "POST:VIEW_COUNT:10")
        sut.read() shouldBe null
        sut.close()

        sut.open(ExecutionContext())
        sut.read() shouldBe PostViewCount(postId = 20L, delta = 6L, cacheKey = "POST:VIEW_COUNT:20")
        sut.read() shouldBe null
    }
}
