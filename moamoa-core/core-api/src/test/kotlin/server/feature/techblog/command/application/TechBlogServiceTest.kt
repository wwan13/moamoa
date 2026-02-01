package server.feature.techblog.command.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.techblog.command.domain.TechBlogRepository
import server.fixture.createTechBlog
import test.UnitTest

class TechBlogServiceTest : UnitTest() {
    @Test
    fun `기술 블로그 키로 조회하면 TechBlogData를 반환한다`() = runTest {
        val techBlogRepository = mockk<TechBlogRepository>()
        val service = TechBlogService(techBlogRepository)
        val techBlogKey = "blog-key"
        val techBlog = createTechBlog(
            id = 1L,
            title = "Blog",
            key = techBlogKey,
            icon = "icon",
            blogUrl = "https://blog.example.com",
            subscriptionCount = 3L
        )

        coEvery { techBlogRepository.findByKey(techBlogKey) } returns techBlog

        val result = service.findByKey(techBlogKey)

        result shouldBe TechBlogData(techBlog)
        coVerify(exactly = 1) { techBlogRepository.findByKey(techBlogKey) }
    }

    @Test
    fun `존재하지 않는 기술 블로그 키면 예외가 발생한다`() = runTest {
        val techBlogRepository = mockk<TechBlogRepository>()
        val service = TechBlogService(techBlogRepository)
        val techBlogKey = "missing-key"

        coEvery { techBlogRepository.findByKey(techBlogKey) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.findByKey(techBlogKey)
        }

        exception.message shouldBe "존재하지 않는 기술 블로그 입니다."
        coVerify(exactly = 1) { techBlogRepository.findByKey(techBlogKey) }
    }

    @Test
    fun `기술 블로그 목록을 제목 오름차순으로 조회해 반환한다`() = runTest {
        val techBlogRepository = mockk<TechBlogRepository>()
        val service = TechBlogService(techBlogRepository)
        val techBlogs = listOf(
            createTechBlog(id = 1L, title = "A Blog", key = "a"),
            createTechBlog(id = 2L, title = "B Blog", key = "b")
        )

        every { techBlogRepository.findAllByOrderByTitleAsc() } returns flowOf(*techBlogs.toTypedArray())

        val result = service.findAll().toList()

        result shouldBe techBlogs.map(::TechBlogData)
        verify(exactly = 1) { techBlogRepository.findAllByOrderByTitleAsc() }
    }
}
