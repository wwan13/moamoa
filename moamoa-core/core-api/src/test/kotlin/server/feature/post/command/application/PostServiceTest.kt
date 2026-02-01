package server.feature.post.command.application

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.infra.cache.PostViewCountCache
import test.UnitTest

class PostServiceTest : UnitTest() {
    @Test
    fun `게시글 조회수 증가 시 캐시를 증가시키고 성공을 반환한다`() = runTest {
        val postViewCountCache = mockk<PostViewCountCache>(relaxed = true)
        val service = PostService(postViewCountCache)
        val postId = 10L

        val result = service.increaseViewCount(postId)

        result.success shouldBe true
        coVerify(exactly = 1) { postViewCountCache.incr(postId) }
    }
}
