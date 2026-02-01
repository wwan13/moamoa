package server.feature.postbookmark.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.member.command.domain.MemberRepository
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmark
import server.feature.postbookmark.domain.PostBookmarkRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.fixture.createPost
import server.global.lock.KeyedMutex
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import test.UnitTest

class PostBookmarkServiceTest : UnitTest() {
    @Test
    fun `기존에 북마크 되어있지 않을 시 북마크를 생성한다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)
        val savedSlot = slot<PostBookmark>()

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns null
        coEvery { postBookmarkRepository.save(capture(savedSlot)) } coAnswers {
            savedSlot.captured.copy(id = 101L)
        }
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.bookmarked shouldBe true
        savedSlot.captured.memberId shouldBe memberId
        savedSlot.captured.postId shouldBe command.postId
    }

    @Test
    fun `기존에 북마크 되어있지 않을 시 PostBookmarkUpdateEvent를 발행하고 bookmarked는 true이다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns null
        coEvery { postBookmarkRepository.save(any()) } coAnswers {
            firstArg<PostBookmark>().copy(id = 101L)
        }
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is PostBookmarkUpdatedEvent &&
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
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)
        val existing = PostBookmark(id = 22L, memberId = memberId, postId = command.postId)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns existing
        coEvery { postBookmarkRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.bookmarked shouldBe false
        coVerify(exactly = 1) { postBookmarkRepository.deleteById(existing.id) }
    }

    @Test
    fun `기존에 북마크 되어 있을 시 PostBookmarkUpdateEvent를 발행하고 bookmarked는 false이다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)
        val existing = PostBookmark(id = 22L, memberId = memberId, postId = command.postId)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns true
        coEvery { postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) } returns existing
        coEvery { postBookmarkRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is PostBookmarkUpdatedEvent &&
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
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns false
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        coVerify(exactly = 0) { postRepository.existsById(any()) }
        coVerify(exactly = 0) { postBookmarkRepository.findByMemberIdAndPostId(any(), any()) }
    }

    @Test
    fun `존재하지 않는 게시글이면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val command = PostBookmarkToggleCommand(postId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { postRepository.existsById(command.postId) } returns false
        coEvery { transactional.invoke<PostBookmarkToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> PostBookmarkToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 게시글 입니다."
        coVerify(exactly = 0) { postBookmarkRepository.findByMemberIdAndPostId(any(), any()) }
    }

    @Test
    fun `북마크가 없으면 빈 결과를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L

        coEvery { postBookmarkRepository.findAllByMemberId(memberId) } returns emptyFlow()

        val result = service.bookmarkedPosts(memberId).toList()

        result shouldBe emptyList()
        verify(exactly = 0) { postRepository.findAllById(any<Iterable<Long>>()) }
    }

    @Test
    fun `북마크가 있으면 게시글을 조회하여 PostData로 변환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val postBookmarkRepository = mockk<PostBookmarkRepository>()
        val postRepository = mockk<PostRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedMutex = KeyedMutex()
        val service = PostBookmarkService(
            transactional,
            postBookmarkRepository,
            postRepository,
            memberRepository,
            keyedMutex
        )

        val memberId = 1L
        val bookmarks = listOf(
            PostBookmark(id = 1L, memberId = memberId, postId = 10L),
            PostBookmark(id = 2L, memberId = memberId, postId = 20L)
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

        coEvery { postBookmarkRepository.findAllByMemberId(memberId) } returns flowOf(*bookmarks.toTypedArray())
        every { postRepository.findAllById(listOf(10L, 20L)) } returns flowOf(*posts.toTypedArray())

        val result = service.bookmarkedPosts(memberId).toList()

        result.map { it.id } shouldBe listOf(10L, 20L)
        result.map { it.key } shouldBe listOf("key-10", "key-20")
    }
}
