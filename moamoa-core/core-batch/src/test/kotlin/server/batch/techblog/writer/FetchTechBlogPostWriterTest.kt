package server.batch.techblog.writer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk
import server.batch.techblog.dto.PostData
import server.queue.QueueMemory
import test.UnitTest
import java.time.LocalDateTime

class FetchTechBlogPostWriterTest : UnitTest() {

    @Test
    fun `첫 write 호출 시 큐를 초기화하고 게시글을 적재한다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = FetchTechBlogPostWriter(queueMemory, 11L)
        val key = "TECH_BLOG:FETCHED_POSTS:11"
        val post1 = postData("p1")
        val post2 = postData("p2")

        coEvery { queueMemory.delete(key) } returns Unit
        coEvery { queueMemory.rPushAll(key, any()) } returns 2L

        sut.write(Chunk(listOf(listOf(post1), listOf(post2))))

        coVerify(exactly = 1) { queueMemory.delete(key) }
        coVerify(exactly = 1) { queueMemory.rPushAll(key, listOf(post1, post2)) }
    }

    @Test
    fun `같은 인스턴스에서 두번째 호출부터는 큐 삭제를 다시 하지 않는다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = FetchTechBlogPostWriter(queueMemory, 12L)
        val key = "TECH_BLOG:FETCHED_POSTS:12"
        val post = postData("p1")

        coEvery { queueMemory.delete(key) } returns Unit
        coEvery { queueMemory.rPushAll(key, any()) } returns 1L

        sut.write(Chunk(listOf(listOf(post))))
        sut.write(Chunk(listOf(listOf(post))))

        coVerify(exactly = 1) { queueMemory.delete(key) }
        coVerify(exactly = 2) { queueMemory.rPushAll(key, listOf(post)) }
    }

    @Test
    fun `빈 chunk면 push를 수행하지 않는다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = FetchTechBlogPostWriter(queueMemory, 13L)
        val key = "TECH_BLOG:FETCHED_POSTS:13"

        coEvery { queueMemory.delete(key) } returns Unit

        sut.write(Chunk(listOf(emptyList())))

        coVerify(exactly = 1) { queueMemory.delete(key) }
        coVerify(exactly = 0) { queueMemory.rPushAll(key, any()) }
    }

    private fun postData(key: String) = PostData(
        key = key,
        title = "title",
        description = "description",
        tags = listOf("kotlin"),
        thumbnail = "thumbnail",
        publishedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        url = "https://example.com/$key",
        techBlogId = 1L
    )
}
