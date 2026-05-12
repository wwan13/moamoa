package server.batch.techblog.processor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlinx.coroutines.flow.flowOf
import server.batch.techblog.dto.TechBlogKey
import server.batch.techblog.monitoring.TechBlogCollectMonitorStore
import server.techblog.TechBlogPostCategory
import server.techblog.TechBlogPostCatetorizer
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import test.UnitTest
import java.time.LocalDateTime

class FetchTechBlogPostProcessorTest : UnitTest() {

    @Test
    fun `source 게시글을 PostData로 변환한다`() {
        val source = mockk<TechBlogSource>()
        val categorizer = mockk<TechBlogPostCatetorizer>()
        val monitorStore = mockk<TechBlogCollectMonitorStore>()
        val sut = FetchTechBlogPostProcessor(source, categorizer, monitorStore, 100L, 10L)
        val techBlogKey = TechBlogKey(id = 7L, techBlogKey = "wanted", title = "Wanted")
        val post = TechBlogPost(
            key = "p1",
            title = "title",
            description = "desc",
            tags = listOf("kotlin"),
            thumbnail = "thumb",
            publishedAt = LocalDateTime.of(2026, 1, 1, 10, 0),
            url = "https://example.com/p1"
        )

        every { categorizer.categorize(post) } returns post.copy(category = TechBlogPostCategory.ENGINEERING)
        coEvery { source.getPosts("wanted", 10) } returns flowOf(post)
        coEvery { monitorStore.recordFetchSuccess(100L, techBlogKey, 1) } returns Unit

        val result = sut.process(techBlogKey)

        result?.size shouldBe 1
        result?.first()?.key shouldBe "p1"
        result?.first()?.categoryId shouldBe 10L
        result?.first()?.techBlogId shouldBe 7L
        coVerify(exactly = 1) { monitorStore.recordFetchSuccess(100L, techBlogKey, 1) }
    }

    @Test
    fun `post limit이 없으면 null을 전달한다`() {
        val source = mockk<TechBlogSource>()
        val categorizer = mockk<TechBlogPostCatetorizer>()
        val monitorStore = mockk<TechBlogCollectMonitorStore>()
        val sut = FetchTechBlogPostProcessor(source, categorizer, monitorStore, 101L, null)
        val techBlogKey = TechBlogKey(id = 8L, techBlogKey = "kakao", title = "Kakao")

        coEvery { source.getPosts("kakao", null) } returns flowOf()
        coEvery { monitorStore.recordFetchSuccess(101L, techBlogKey, 0) } returns Unit

        val result = sut.process(techBlogKey)

        result shouldBe emptyList()
        coVerify(exactly = 1) { monitorStore.recordFetchSuccess(101L, techBlogKey, 0) }
    }

    @Test
    fun `source fetch 실패 시 예외를 삼키고 실패를 기록한다`() {
        val source = mockk<TechBlogSource>()
        val categorizer = mockk<TechBlogPostCatetorizer>()
        val monitorStore = mockk<TechBlogCollectMonitorStore>()
        val sut = FetchTechBlogPostProcessor(source, categorizer, monitorStore, 102L, 5L)
        val techBlogKey = TechBlogKey(id = 9L, techBlogKey = "bad", title = "Bad")
        val throwable = IllegalStateException("boom")

        coEvery { source.getPosts("bad", 5) } throws throwable
        coEvery { monitorStore.recordFetchFailure(102L, techBlogKey, throwable) } returns Unit

        val result = sut.process(techBlogKey)

        result shouldBe emptyList()
        coVerify(exactly = 1) { monitorStore.recordFetchFailure(102L, techBlogKey, throwable) }
    }

    @Test
    fun `모니터링 저장 실패가 나도 source fetch 성공 결과는 반환한다`() {
        val source = mockk<TechBlogSource>()
        val categorizer = mockk<TechBlogPostCatetorizer>()
        val monitorStore = mockk<TechBlogCollectMonitorStore>()
        val sut = FetchTechBlogPostProcessor(source, categorizer, monitorStore, 103L, 10L)
        val techBlogKey = TechBlogKey(id = 10L, techBlogKey = "ok", title = "OK")
        val post = TechBlogPost(
            key = "p2",
            title = "title-2",
            description = "desc-2",
            tags = listOf("kotlin"),
            thumbnail = "thumb-2",
            publishedAt = LocalDateTime.of(2026, 1, 1, 11, 0),
            url = "https://example.com/p2"
        )

        every { categorizer.categorize(post) } returns post.copy(category = TechBlogPostCategory.ENGINEERING)
        coEvery { source.getPosts("ok", 10) } returns flowOf(post)
        coEvery { monitorStore.recordFetchSuccess(103L, techBlogKey, 1) } throws IllegalStateException("redis down")

        val result = sut.process(techBlogKey)

        result?.size shouldBe 1
        result?.first()?.key shouldBe "p2"
    }

    @Test
    fun `카테고리 매칭 실패 시 999로 저장한다`() {
        val source = mockk<TechBlogSource>()
        val categorizer = mockk<TechBlogPostCatetorizer>()
        val monitorStore = mockk<TechBlogCollectMonitorStore>()
        val sut = FetchTechBlogPostProcessor(source, categorizer, monitorStore, 104L, 10L)
        val techBlogKey = TechBlogKey(id = 11L, techBlogKey = "none", title = "None")
        val post = TechBlogPost(
            key = "p3",
            title = "title-3",
            description = "desc-3",
            tags = listOf("unknown"),
            thumbnail = "thumb-3",
            publishedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            url = "https://example.com/p3"
        )

        every { categorizer.categorize(post) } returns post.copy(category = TechBlogPostCategory.UNDEFINED)
        coEvery { source.getPosts("none", 10) } returns flowOf(post)
        coEvery { monitorStore.recordFetchSuccess(104L, techBlogKey, 1) } returns Unit

        val result = sut.process(techBlogKey)

        result?.size shouldBe 1
        result?.first()?.categoryId shouldBe 999L
    }
}
