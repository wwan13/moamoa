package server.batch.techblog.processor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlinx.coroutines.flow.flowOf
import server.batch.techblog.dto.TechBlogKey
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource
import server.techblog.TechBlogSources
import test.UnitTest
import java.time.LocalDateTime

class FetchTechBlogPostProcessorTest : UnitTest() {

    @Test
    fun `source 게시글을 PostData로 변환한다`() {
        val sources = mockk<TechBlogSources>()
        val source = mockk<TechBlogSource>()
        val sut = FetchTechBlogPostProcessor(sources, 10L)
        val techBlogKey = TechBlogKey(id = 7L, techBlogKey = "wanted")
        val post = TechBlogPost(
            key = "p1",
            title = "title",
            description = "desc",
            tags = listOf("kotlin"),
            thumbnail = "thumb",
            publishedAt = LocalDateTime.of(2026, 1, 1, 10, 0),
            url = "https://example.com/p1"
        )

        every { sources["wanted"] } returns source
        coEvery { source.getPosts(10) } returns flowOf(post)

        val result = sut.process(techBlogKey)

        result?.size shouldBe 1
        result?.first()?.key shouldBe "p1"
        result?.first()?.techBlogId shouldBe 7L
    }

    @Test
    fun `post limit이 없으면 null을 전달한다`() {
        val sources = mockk<TechBlogSources>()
        val source = mockk<TechBlogSource>()
        val sut = FetchTechBlogPostProcessor(sources, null)
        val techBlogKey = TechBlogKey(id = 8L, techBlogKey = "kakao")

        every { sources["kakao"] } returns source
        coEvery { source.getPosts(null) } returns flowOf()

        val result = sut.process(techBlogKey)

        result shouldBe emptyList()
    }
}
