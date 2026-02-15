package server.batch.post.writer

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.batch.common.transaction.AfterCommitExecutor
import server.batch.post.dto.PostViewCount
import server.cache.CacheMemory
import server.set.SetMemory
import test.UnitTest

class UpdatePostViewCountWriterTest : UnitTest() {

    @Test
    fun `증가분을 DB에 반영하고 커밋 후 redis 보정을 수행한다`() = runTest {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val afterCommitExecutor = mockk<AfterCommitExecutor>()
        val cacheMemory = mockk<CacheMemory>()
        val setMemory = mockk<SetMemory>()
        val sut = UpdatePostViewCountWriter(jdbc, afterCommitExecutor, cacheMemory, setMemory)

        val actionSlot = slot<suspend () -> Unit>()
        val items = listOf(
            PostViewCount(postId = 1L, delta = 3L, cacheKey = "POST:VIEW_COUNT:1"),
            PostViewCount(postId = 2L, delta = 2L, cacheKey = "POST:VIEW_COUNT:2")
        )

        every { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) } returns intArrayOf(1, 1)
        every { afterCommitExecutor.execute(capture(actionSlot)) } just runs
        coEvery { cacheMemory.decrBy("POST:VIEW_COUNT:1", 3L) } returns 0L
        coEvery { cacheMemory.decrBy("POST:VIEW_COUNT:2", 2L) } returns 5L
        coEvery { cacheMemory.evict("POST:VIEW_COUNT:1") } returns Unit
        coEvery { setMemory.remove("POST:VIEW_COUNT:DIRTY_SET", "1") } returns 1L

        sut.write(Chunk(items))

        verify(exactly = 1) { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) }
        verify(exactly = 1) { afterCommitExecutor.execute(any()) }

        actionSlot.captured.invoke()

        coVerify(exactly = 1) { cacheMemory.decrBy("POST:VIEW_COUNT:1", 3L) }
        coVerify(exactly = 1) { cacheMemory.decrBy("POST:VIEW_COUNT:2", 2L) }
        coVerify(exactly = 1) { cacheMemory.evict("POST:VIEW_COUNT:1") }
        coVerify(exactly = 0) { cacheMemory.evict("POST:VIEW_COUNT:2") }
        coVerify(exactly = 1) { setMemory.remove("POST:VIEW_COUNT:DIRTY_SET", "1") }
        coVerify(exactly = 0) { setMemory.remove("POST:VIEW_COUNT:DIRTY_SET", "2") }
    }

    @Test
    fun `빈 chunk면 아무 작업도 하지 않는다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val afterCommitExecutor = mockk<AfterCommitExecutor>()
        val cacheMemory = mockk<CacheMemory>()
        val setMemory = mockk<SetMemory>()
        val sut = UpdatePostViewCountWriter(jdbc, afterCommitExecutor, cacheMemory, setMemory)

        sut.write(Chunk(emptyList()))

        verify(exactly = 0) { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) }
        verify(exactly = 0) { afterCommitExecutor.execute(any()) }
    }

    @Test
    fun `redis 보정 실패는 예외를 전파하지 않는다`() = runTest {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val afterCommitExecutor = mockk<AfterCommitExecutor>()
        val cacheMemory = mockk<CacheMemory>()
        val setMemory = mockk<SetMemory>()
        val sut = UpdatePostViewCountWriter(jdbc, afterCommitExecutor, cacheMemory, setMemory)

        val actionSlot = slot<suspend () -> Unit>()
        val items = listOf(PostViewCount(postId = 1L, delta = 3L, cacheKey = "POST:VIEW_COUNT:1"))

        every { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) } returns intArrayOf(1)
        every { afterCommitExecutor.execute(capture(actionSlot)) } just runs
        coEvery { cacheMemory.decrBy("POST:VIEW_COUNT:1", 3L) } throws IllegalStateException("redis down")

        sut.write(Chunk(items))

        var thrown: Throwable? = null
        runCatching {
            actionSlot.captured.invoke()
        }.onFailure { thrown = it }

        thrown shouldBe null
    }
}
