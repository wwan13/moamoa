package server.admin.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.Test
import server.admin.global.jdsl.AdminJdslExecutor
import test.UnitTest
import java.time.LocalDateTime

class AdminPostQueryServiceTest : UnitTest() {

    @Test
    fun `techBlogIds가 빈 집합이면 조회 없이 빈 결과를 반환한다`() {
        val jdslExecutor = mockk<AdminJdslExecutor>(relaxed = true)
        val service = AdminPostQueryService(jdslExecutor)

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
        val jdslExecutor = mockk<AdminJdslExecutor>()
        val countQuery = mockk<TypedQuery<Long>>(relaxed = true)
        val postQuery = mockk<TypedQuery<AdminPostRow>>(relaxed = true)
        val tagQuery = mockk<TypedQuery<AdminPostTagRow>>(relaxed = true)
        val service = AdminPostQueryService(jdslExecutor)

        every { jdslExecutor.createQuery(any(), Long::class.javaObjectType, any(), any()) } returns countQuery
        every { jdslExecutor.createQuery(any(), AdminPostRow::class.java, any(), any()) } returns postQuery
        every { jdslExecutor.createQuery(any(), AdminPostTagRow::class.java, any(), any()) } returns tagQuery
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
