package server.core.feature.bookmark.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.post.domain.PostRepository
import server.core.fixture.createPost
import server.core.infra.db.transaction.HandleTransactionEvent
import server.messaging.MessageChannel
import server.messaging.SubscriptionDefinition
import test.UnitTest

class BookmarkCountServiceTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 스트림과 타입 정보를 포함한다`() {
        val stream = SubscriptionDefinition(
            MessageChannel("post-bookmark"), "post-bookmark-group"
        )
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = BookmarkCountService(
            stream,
            postRepository,
            HandleTransactionEvent(newTxManager())
        )

        val handler = service.bookmarkUpdatedCountCalculate()

        handler.subscription shouldBe stream
        handler.type shouldBe BookmarkUpdatedEvent::class.java.simpleName
        handler.payloadClass shouldBe BookmarkUpdatedEvent::class.java
    }

    @Test
    fun `북마크가 추가되면 카운트를 증가시킨다`() = runTest {
        val stream = SubscriptionDefinition(
            MessageChannel("post-bookmark"), "post-bookmark-group"
        )
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = BookmarkCountService(
            stream,
            postRepository,
            HandleTransactionEvent(newTxManager())
        )
        val post = createPost(id = 10L, bookmarkCount = 3L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = BookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = true)

        handler.handler(event)

        post.bookmarkCount shouldBe 4L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }

    @Test
    fun `북마크가 해제되면 카운트를 감소시킨다`() = runTest {
        val stream = SubscriptionDefinition(
            MessageChannel("post-bookmark"), "post-bookmark-group"
        )
        val postRepository = mockk<PostRepository>(relaxed = true)
        val service = BookmarkCountService(
            stream,
            postRepository,
            HandleTransactionEvent(newTxManager())
        )
        val post = createPost(id = 10L, bookmarkCount = 1L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = BookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = false)

        handler.handler(event)

        post.bookmarkCount shouldBe 0L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }

    private fun newTxManager(): PlatformTransactionManager {
        val txManager = mockk<PlatformTransactionManager>()
        val status = mockk<TransactionStatus>(relaxed = true)
        every { txManager.getTransaction(any()) } returns status
        every { txManager.commit(status) } just runs
        every { txManager.rollback(status) } just runs
        return txManager
    }
}
