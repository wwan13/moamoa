package server.batch.techblog.writer

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
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import server.batch.common.transaction.AfterCommitExecutor
import server.batch.techblog.dto.PostData
import server.cache.CacheMemory
import server.queue.QueueMemory
import test.UnitTest
import java.time.LocalDateTime

class PersistTechBlogPostWriterTest : UnitTest() {

    @Test
    fun `빈 chunk면 아무 작업도 수행하지 않는다`() {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val afterCommitExecutor = mockk<AfterCommitExecutor>()
        val queueMemory = mockk<QueueMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = PersistTechBlogPostWriter(jdbc, afterCommitExecutor, queueMemory, cacheMemory)

        sut.write(Chunk(listOf(emptyList())))

        verify(exactly = 0) { jdbc.queryForObject(any<String>(), any<Map<String, *>>(), any<Class<*>>()) }
        verify(exactly = 0) { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) }
        verify(exactly = 0) { afterCommitExecutor.execute(any()) }
    }

    @Test
    fun `게시글이 있으면 업서트와 after commit 후처리를 수행한다`() = runTest {
        val jdbc = mockk<NamedParameterJdbcTemplate>()
        val afterCommitExecutor = mockk<AfterCommitExecutor>()
        val queueMemory = mockk<QueueMemory>()
        val cacheMemory = mockk<CacheMemory>()
        val sut = PersistTechBlogPostWriter(jdbc, afterCommitExecutor, queueMemory, cacheMemory)

        val startedAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        val endedAt = LocalDateTime.of(2026, 1, 1, 0, 1)
        val actionSlot = slot<suspend () -> Unit>()
        val post1 = postData("p1")
        val post2 = postData("p2")

        every {
            jdbc.queryForObject("SELECT NOW()", any<Map<String, *>>(), LocalDateTime::class.java)
        } returnsMany listOf(startedAt, endedAt)
        every { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) } returns intArrayOf(1)
        every { afterCommitExecutor.execute(capture(actionSlot)) } just runs
        every { jdbc.query(any<String>(), any<Map<String, *>>(), any<RowMapper<Any>>()) } answers {
            val sql = firstArg<String>()
            when {
                sql.contains("SELECT id, title") -> listOf("kotlin" to 10L, "spring" to 11L)
                sql.contains("SELECT id, tech_blog_id, post_key") -> listOf(
                    (1L to "p1") to 100L,
                    (1L to "p2") to 101L
                )
                sql.contains("SELECT id") && sql.contains("created_at") -> listOf(100L, 101L)
                else -> emptyList<Any>()
            }
        }
        coEvery { queueMemory.delete("NEW_POST_IDS") } returns Unit
        coEvery { queueMemory.rPushAll("NEW_POST_IDS", setOf(100L, 101L)) } returns 2L
        coEvery { cacheMemory.evictByPrefix("POST:LIST:") } returns Unit

        sut.write(Chunk(listOf(listOf(post1, post2))))

        verify(exactly = 2) {
            jdbc.queryForObject("SELECT NOW()", any<Map<String, *>>(), LocalDateTime::class.java)
        }
        verify(exactly = 3) { jdbc.batchUpdate(any<String>(), any<Array<Map<String, *>>>()) }
        verify(exactly = 3) { jdbc.query(any<String>(), any<Map<String, *>>(), any<RowMapper<Any>>()) }
        verify(exactly = 1) { afterCommitExecutor.execute(any()) }
        actionSlot.isCaptured shouldBe true

        actionSlot.captured.invoke()

        coVerify(exactly = 1) { queueMemory.delete("NEW_POST_IDS") }
        coVerify(exactly = 1) { queueMemory.rPushAll("NEW_POST_IDS", setOf(100L, 101L)) }
        coVerify(exactly = 1) { cacheMemory.evictByPrefix("POST:LIST:") }
    }

    private fun postData(key: String) = PostData(
        key = key,
        title = "title-$key",
        description = "description-$key",
        tags = listOf("Kotlin", "Spring"),
        thumbnail = "thumbnail-$key",
        publishedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        url = "https://example.com/$key",
        techBlogId = 1L
    )
}
