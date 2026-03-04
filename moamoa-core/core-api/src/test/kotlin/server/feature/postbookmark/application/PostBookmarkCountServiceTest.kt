package server.feature.postbookmark.application

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.fixture.createPost
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import server.shared.messaging.MessageChannel
import server.shared.messaging.SubscriptionDefinition
import test.UnitTest

class PostBookmarkCountServiceTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 스트림과 타입 정보를 포함한다`() {
        val stream = SubscriptionDefinition(MessageChannel("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val transactional = mockk<Transactional>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository, transactional)

        val handler = service.bookmarkUpdatedCountCalculate()

        handler.subscription shouldBe stream
        handler.type shouldBe PostBookmarkUpdatedEvent::class.java.simpleName
        handler.payloadClass shouldBe PostBookmarkUpdatedEvent::class.java
    }

    @Test
    fun `북마크가 추가되면 카운트를 증가시킨다`() = runTest {
        val stream = SubscriptionDefinition(MessageChannel("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val transactional = mockk<Transactional>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository, transactional)
        val post = createPost(id = 10L, bookmarkCount = 3L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)
        every { transactional.invoke<Unit>(any(), any()) } answers {
            val block = secondArg<TransactionScope.() -> Unit>()
            block.invoke(transactionScope)
        }

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = PostBookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = true)

        handler.handler(event)

        post.bookmarkCount shouldBe 4L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }

    @Test
    fun `북마크가 해제되면 카운트를 감소시킨다`() = runTest {
        val stream = SubscriptionDefinition(MessageChannel("post-bookmark"), "post-bookmark-group")
        val postRepository = mockk<PostRepository>(relaxed = true)
        val transactional = mockk<Transactional>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val service = PostBookmarkCountService(stream, postRepository, transactional)
        val post = createPost(id = 10L, bookmarkCount = 1L)
        every { postRepository.findById(10L) } returns java.util.Optional.of(post)
        every { transactional.invoke<Unit>(any(), any()) } answers {
            val block = secondArg<TransactionScope.() -> Unit>()
            block.invoke(transactionScope)
        }

        val handler = service.bookmarkUpdatedCountCalculate()
        val event = PostBookmarkUpdatedEvent(memberId = 1L, postId = 10L, bookmarked = false)

        handler.handler(event)

        post.bookmarkCount shouldBe 0L
        verify(exactly = 1) { postRepository.findById(event.postId) }
    }
}
