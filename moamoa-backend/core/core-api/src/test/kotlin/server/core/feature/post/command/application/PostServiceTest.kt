package server.core.feature.post.command.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.core.feature.post.application.PostService
import server.core.feature.post.domain.PostRepository
import server.core.feature.post.infra.PostViewCountCache
import server.core.fixture.createPost
import test.UnitTest

class PostServiceTest : UnitTest() {
    @Test
    fun `게시글을 조회하면 조회수를 증가시킨다`() {
        val postRepository = mockk<PostRepository>()
        val postViewCountCache = mockk<PostViewCountCache>(relaxed = true)
        val service = PostService(postRepository, postViewCountCache)
        val postId = 10L
        val post = createPost(id = postId, title = "테스트 게시글")
        every { postRepository.findById(postId) } returns java.util.Optional.of(post)

        val result = service.findById(postId)

        result.id shouldBe postId
        result.title shouldBe "테스트 게시글"
        verify(exactly = 1) { postRepository.findById(postId) }
        verify(exactly = 1) { postViewCountCache.incr(postId) }
    }
}
