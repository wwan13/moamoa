package server.admin.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.Test
import test.UnitTest
import java.time.LocalDateTime

class AdminPostQueryServiceTest : UnitTest() {

    @Test
    fun `techBlogIds가 빈 집합이면 조회 없이 빈 결과를 반환한다`() {
        val entityManager = mockk<EntityManager>(relaxed = true)
        val service = AdminPostQueryService(entityManager)

        val result = service.findByConditions(
            AdminPostQueryConditions(
                page = 1L,
                size = 20L,
                query = null,
                categoryId = null,
                techBlogIds = emptySet(),
            )
        )

        result.meta.totalCount shouldBe 0L
        result.meta.totalPages shouldBe 0L
        result.posts shouldBe emptyList()
    }

    @Test
    fun `게시글과 태그를 조합해 목록을 반환한다`() {
        val entityManager = mockk<EntityManager>()
        val countQuery = mockk<TypedQuery<Long>>(relaxed = true)
        val postQuery = mockk<TypedQuery<AdminPostRow>>(relaxed = true)
        val tagQuery = mockk<TypedQuery<AdminPostTagRow>>(relaxed = true)
        val service = AdminPostQueryService(entityManager)

        every { entityManager.createQuery(any<String>(), Long::class.javaObjectType) } returns countQuery
        every { entityManager.createQuery(any<String>(), AdminPostRow::class.java) } returns postQuery
        every { entityManager.createQuery(any<String>(), AdminPostTagRow::class.java) } returns tagQuery
        every { countQuery.resultList } returns listOf(1L)
        every { postQuery.resultList } returns listOf(
            AdminPostRow(
                postId = 10L,
                key = "post-10",
                title = "title",
                description = "description",
                thumbnail = "thumbnail",
                url = "https://example.com/post-10",
                publishedAt = LocalDateTime.of(2026, 3, 1, 12, 0),
                categoryId = 3L,
                techBlogId = 5L,
                techBlogTitle = "blog",
                techBlogIcon = "icon",
                techBlogBlogUrl = "https://blog.example.com",
                techBlogKey = "blog-key",
            )
        )
        every { tagQuery.resultList } returns listOf(
            AdminPostTagRow(
                postId = 10L,
                tagId = 100L,
                tagTitle = "kotlin",
            )
        )

        val result = service.findByConditions(
            AdminPostQueryConditions(
                page = 1L,
                size = 20L,
                query = "kot",
                categoryId = 3L,
                techBlogIds = setOf(5L),
            )
        )

        result.meta.totalCount shouldBe 1L
        result.meta.totalPages shouldBe 1L
        result.posts.single().tags.single().title shouldBe "kotlin"
        result.posts.single().techBlog.id shouldBe 5L
    }
}
