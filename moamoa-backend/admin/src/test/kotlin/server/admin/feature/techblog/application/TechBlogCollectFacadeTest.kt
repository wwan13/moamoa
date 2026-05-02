package server.admin.feature.techblog.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.admin.feature.techblog.domain.AdminTechBlog
import server.admin.feature.techblog.domain.AdminTechBlogRepository
import server.admin.feature.techblog.infra.TechBlogCollector
import server.techblog.TechBlogPost
import test.UnitTest
import java.time.LocalDateTime
import java.util.Optional

class TechBlogCollectFacadeTest : UnitTest() {

    @Test
    fun `포스트 수집 시 수집기와 서비스 저장을 순서대로 호출한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val techBlogCollector = mockk<TechBlogCollector>()
        val techBlogService = mockk<AdminTechBlogService>()
        val facade = TechBlogCollectFacade(
            techBlogRepository = techBlogRepository,
            techBlogCollector = techBlogCollector,
            techBlogService = techBlogService,
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")
        val fetchedPosts = listOf(createTechBlogPost(key = "p1"))
        val expected = AdminCollectPostsResult(
            techBlog = AdminTechBlogData(techBlog),
            newPostCount = 1,
            updatedPostCount = 0,
        )

        every { techBlogRepository.findById(1L) } returns Optional.of(techBlog)
        every { techBlogCollector.collect("gabia") } returns fetchedPosts
        every { techBlogService.createPosts(1L, fetchedPosts) } returns expected

        val result = facade.collectPosts(AdminCollectPostsCommand(techBlogId = 1L))

        result shouldBe expected
        verify(exactly = 1) { techBlogCollector.collect("gabia") }
        verify(exactly = 1) { techBlogService.createPosts(1L, fetchedPosts) }
    }

    @Test
    fun `tech blog가 존재하지 않으면 예외가 발생한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val facade = TechBlogCollectFacade(
            techBlogRepository = techBlogRepository,
            techBlogCollector = mockk(),
            techBlogService = mockk(),
        )

        every { techBlogRepository.findById(1L) } returns Optional.empty()

        val exception = shouldThrow<NoSuchElementException> {
            facade.collectPosts(AdminCollectPostsCommand(techBlogId = 1L))
        }

        exception.message shouldBe "존재하지 않는 tech blog 입니다."
    }

    private fun createAdminTechBlog(
        id: Long = 0L,
        title: String = "가비아",
        key: String = "gabia",
        blogUrl: String = "https://library.gabia.com",
        icon: String = "https://library.gabia.com/icon.png",
    ) = AdminTechBlog(
        id = id,
        title = title,
        key = key,
        blogUrl = blogUrl,
        icon = icon,
    )

    private fun createTechBlogPost(
        key: String = "key",
        title: String = "title",
        description: String = "description",
        thumbnail: String = "thumbnail",
        url: String = "https://example.com",
        publishedAt: LocalDateTime = LocalDateTime.parse("2026-04-01T00:00:00"),
        tags: List<String> = emptyList(),
    ) = TechBlogPost(
        key = key,
        title = title,
        description = description,
        tags = tags,
        thumbnail = thumbnail,
        publishedAt = publishedAt,
        url = url,
    )
}
