package server.admin.feature.post.command.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.admin.feature.post.command.domain.AdminPostRepository
import server.admin.fixture.createAdminPost
import server.admin.infra.db.transaction.AdminTransactional
import test.UnitTest

class AdminPostServiceTest : UnitTest() {

    @Test
    fun `카테고리 업데이트 시 게시글의 categoryId를 변경하고 성공을 반환한다`() = runTest {
        val transactional = mockk<AdminTransactional>()
        val postRepository = mockk<AdminPostRepository>()
        val service = AdminPostService(transactional, postRepository)

        val postId = 10L
        val command = AdminUpdateCategoryCommand(categoryId = 200L)
        val post = createAdminPost(id = postId, categoryId = 100L)

        coEvery { postRepository.findById(postId) } returns post
        coEvery { postRepository.save(any()) } answers { firstArg() }
        coEvery { transactional.invoke<AdminUpdateCategoryResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend () -> AdminUpdateCategoryResult>()
            block.invoke()
        }

        val result = service.updateCategory(postId, command)

        result.success shouldBe true
        coVerify(exactly = 1) {
            postRepository.save(match { it.id == postId && it.categoryId == command.categoryId })
        }
    }

    @Test
    fun `게시글이 존재하지 않으면 예외가 발생한다`() = runTest {
        val transactional = mockk<AdminTransactional>()
        val postRepository = mockk<AdminPostRepository>()
        val service = AdminPostService(transactional, postRepository)

        val postId = 10L
        val command = AdminUpdateCategoryCommand(categoryId = 200L)

        coEvery { postRepository.findById(postId) } returns null
        coEvery { transactional.invoke<AdminUpdateCategoryResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend () -> AdminUpdateCategoryResult>()
            block.invoke()
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.updateCategory(postId, command)
        }

        exception.message shouldBe "존재하지 않는 게시글 입니다."
        coVerify(exactly = 0) { postRepository.save(any()) }
    }
}
