package server.core.feature.bookmark.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.bookmark.application.BookmarkService
import server.core.feature.bookmark.application.BookmarkToggleCommand
import server.core.feature.bookmark.application.BookmarkToggleResult
import server.core.feature.member.domain.MemberRepository
import server.core.feature.post.domain.PostRepository
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.fixture.createPost
import server.core.infra.db.transaction.TransactionScope
import server.core.infra.db.transaction.Transactional
import test.UnitTest

class BookmarkServiceTest : UnitTest() {
    @Test
    fun `기존에 북마크 되어있지 않을 시 북마크를 생성한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)
        val savedSlot = slot<Bookmark>()

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns null
        coEvery { bookmarkRepository.save(capture(savedSlot)) } coAnswers {
            Bookmark(id = 101L, memberId = savedSlot.captured.memberId, postId = savedSlot.captured.postId)
        }
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.bookmarked shouldBe true
        savedSlot.captured.memberId shouldBe memberId
        savedSlot.captured.postId shouldBe command.postId
    }

    @Test
    fun `기존에 북마크 되어있지 않을 시 BookmarkUpdateEvent를 발행하고 bookmarked는 true이다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns null
        coEvery { bookmarkRepository.save(any()) } coAnswers {
            val bookmark = firstArg<Bookmark>()
            Bookmark(id = 101L, memberId = bookmark.memberId, postId = bookmark.postId)
        }
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is BookmarkUpdatedEvent &&
                        it.memberId == memberId &&
                        it.postId == command.postId &&
                        it.bookmarked
                }
            )
        }
    }

    @Test
    fun `기존에 북마크 되어 있을 시 북마크를 제거한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)
        val existing = Bookmark(id = 22L, memberId = memberId, postId = command.postId)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns existing
        coEvery { bookmarkRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.bookmarked shouldBe false
        coVerify(exactly = 1) { bookmarkRepository.deleteById(existing.id) }
    }

    @Test
    fun `기존에 북마크 되어 있을 시 BookmarkUpdateEvent를 발행하고 bookmarked는 false이다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)
        val existing = Bookmark(id = 22L, memberId = memberId, postId = command.postId)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns existing
        coEvery { bookmarkRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is BookmarkUpdatedEvent &&
                        it.memberId == memberId &&
                        it.postId == command.postId &&
                        !it.bookmarked
                }
            )
        }
    }

    @Test
    fun `존재하지 않는 사용자면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns false
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        coVerify(exactly = 0) { postRepository.existsById(any()) }
        coVerify(exactly = 0) { bookmarkRepository.findByMemberIdAndPostId(any(), any()) }
    }

    @Test
    fun `존재하지 않는 게시글이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = BookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns false
        coEvery { transactional.invoke<BookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> BookmarkToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 게시글 입니다."
        coVerify(exactly = 0) { bookmarkRepository.findByMemberIdAndPostId(any(), any()) }
    }

    @Test
    fun `북마크가 없으면 빈 결과를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L

        coEvery { bookmarkRepository.findAllByMemberId(memberId) } returns emptyList()

        val result = service.bookmarkedPosts(memberId)

        result shouldBe emptyList()
        verify(exactly = 0) { postRepository.findAllById(any<Iterable<Long>>()) }
    }

    @Test
    fun `북마크가 있으면 게시글을 조회하여 PostData로 변환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val bookmarkRepository = mockk<BookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedLock = passThroughKeyedLock()
        val service = BookmarkService(
            transactional,
            bookmarkRepository,
            postRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val bookmarks = listOf(
            Bookmark(id = 1L, memberId = memberId, postId = 10L),
            Bookmark(id = 2L, memberId = memberId, postId = 20L)
        )
        val posts = listOf(
            createPost(
                id = 10L,
                key = "key-10"
            ),
            createPost(
                id = 20L,
                key = "key-20"
            )
        )

        coEvery { bookmarkRepository.findAllByMemberId(memberId) } returns bookmarks
        every { postRepository.findAllById(listOf(10L, 20L)) } returns posts

        val result = service.bookmarkedPosts(memberId)

        result.map { it.id } shouldBe listOf(10L, 20L)
        result.map { it.key } shouldBe listOf("key-10", "key-20")
    }

    private fun passThroughKeyedLock(): server.lock.KeyedLock = object : server.lock.KeyedLock {
        override fun <T> withLock(key: String, block: () -> T): T = block()
    }
}
