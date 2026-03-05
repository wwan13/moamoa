package server.batch.techblog.reader

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import server.batch.techblog.dto.PostData
import server.queue.QueueMemory
import test.UnitTest
import java.time.LocalDateTime

class PersistTechBlogPostReaderTest : UnitTest() {

    @Test
    fun `큐가 비어 있으면 null을 반환한다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = PersistTechBlogPostReader(queueMemory, 100L)
        val key = "TECH_BLOG:FETCHED_POSTS:100"

        coEvery { queueMemory.drain(key, PostData::class.java, 200) } returns emptyList()

        val result = sut.read()

        result.shouldBeNull()
        coVerify(exactly = 1) { queueMemory.drain(key, PostData::class.java, 200) }
    }

    @Test
    fun `큐에 데이터가 있으면 목록을 반환한다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = PersistTechBlogPostReader(queueMemory, 101L)
        val key = "TECH_BLOG:FETCHED_POSTS:101"
        val post = postData("post-1")

        coEvery { queueMemory.drain(key, PostData::class.java, 200) } returns listOf(post)

        val result = sut.read()

        result shouldBe listOf(post)
        coVerify(exactly = 1) { queueMemory.drain(key, PostData::class.java, 200) }
    }

    @Test
    fun `run id가 없으면 unknown 키를 사용한다`() {
        val queueMemory = mockk<QueueMemory>()
        val sut = PersistTechBlogPostReader(queueMemory, null)
        val key = "TECH_BLOG:FETCHED_POSTS:unknown"

        coEvery { queueMemory.drain(key, PostData::class.java, 200) } returns emptyList()

        sut.read()

        coVerify(exactly = 1) { queueMemory.drain(key, PostData::class.java, 200) }
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
