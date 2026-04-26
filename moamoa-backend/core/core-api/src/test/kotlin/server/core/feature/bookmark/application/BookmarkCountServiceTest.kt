package server.core.feature.bookmark.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.post.domain.PostRepository
import server.core.fixture.createPost
import test.UnitTest

class BookmarkCountServiceTest : UnitTest() {
    @Test
    fun `북마크가 추가되면 카운트를 증가시킨다`() = runTest {
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = BookmarkCountService(postRepository)
        val post = createPost(id = 10L, bookmarkCount = 3L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)

        val event = BookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = true)

        service.bookmarkUpdatedCountCalculate(event)

        post.bookmarkCount shouldBe 4L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }

    @Test
    fun `북마크가 해제되면 카운트를 감소시킨다`() = runTest {
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = BookmarkCountService(postRepository)
        val post = createPost(id = 10L, bookmarkCount = 1L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)

        val event = BookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = false)

        service.bookmarkUpdatedCountCalculate(event)

        post.bookmarkCount shouldBe 0L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }
}
