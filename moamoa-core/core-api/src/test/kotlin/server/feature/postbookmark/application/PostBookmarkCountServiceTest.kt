package server.feature.postbookmark.application

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.messaging.StreamDefinition
import server.messaging.StreamTopic
import test.UnitTest

class PostBookmarkCountServiceTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 스트림과 타입 정보를 포함한다`() {
        val stream = StreamDefinition(StreamTopic("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository)

        val handler = service.bookmarkUpdatedCountCalculate()

        handler.stream shouldBe stream
        handler.type shouldBe PostBookmarkUpdatedEvent::class.java.simpleName
        handler.payloadClass shouldBe PostBookmarkUpdatedEvent::class.java
    }

    @Test
    fun `북마크가 추가되면 카운트를 증가시킨다`() = runTest {
        val stream = StreamDefinition(StreamTopic("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository)

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = PostBookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = true)

        handler.handler(event)

        coVerify(exactly = 1) { postRepository.incrementBookmarkCount(event.postId, 1L) }
    }

    @Test
    fun `북마크가 해제되면 카운트를 감소시킨다`() = runTest {
        val stream = StreamDefinition(StreamTopic("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository)

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = PostBookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = false)

        handler.handler(event)

        coVerify(exactly = 1) { postRepository.incrementBookmarkCount(event.postId, -1L) }
    }
}
