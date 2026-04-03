package server.admin.feature.post.command.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.Optional
import server.admin.feature.post.command.domain.AdminPostRepository
import server.admin.fixture.createAdminPost
import test.UnitTest

class AdminPostServiceTest : UnitTest() {

    @Test
    fun `카테고리 업데이트 시 게시글의 categoryId를 변경한다`() = runTest {
        val postRepository = mockk<AdminPostRepository>()
        val service = AdminPostService(postRepository)

        val postId = 10L
        val command = AdminUpdateCategoryCommand(categoryId = 200L)
        val post = createAdminPost(id = postId, categoryId = 100L)

        every { postRepository.findById(postId) } returns Optional.of(post)

        service.updateCategory(postId, command)

        post.categoryId shouldBe command.categoryId
    }

    @Test
    fun `게시글이 존재하지 않으면 예외가 발생한다`() = runTest {
        val postRepository = mockk<AdminPostRepository>()
        val service = AdminPostService(postRepository)

        val postId = 10L
        val command = AdminUpdateCategoryCommand(categoryId = 200L)

        every { postRepository.findById(postId) } returns Optional.empty()

        val exception = shouldThrow<IllegalArgumentException> {
            service.updateCategory(postId, command)
        }

        exception.message shouldBe "존재하지 않는 게시글 입니다."
        verify(exactly = 1) { postRepository.findById(postId) }
    }
}
