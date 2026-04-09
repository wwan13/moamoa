package server.core.feature.bookmark.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.bookmark.infra.BookmarkEventPublisher
import server.core.feature.bookmark.infra.BookmarkLock
import server.core.feature.member.domain.MemberRepository
import server.core.feature.post.application.PostData
import server.core.feature.post.domain.PostRepository
import server.core.global.security.UnauthorizedException
import server.global.logging.biz

@Service
@Transactional
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val bookmarkLock: BookmarkLock,
    private val bookmarkEventPublisher: BookmarkEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun bookmark(
        command: BookmarkCommand,
        memberId: Long
    ) = bookmarkLock.withLock(memberId, command.postId) {
        if (!memberRepository.existsById(memberId)) {
            throw UnauthorizedException()
        }
        if (!postRepository.existsById(command.postId)) {
            throw NoSuchElementException("존재하지 않는 게시글 입니다.")
        }

        if (bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId) != null) {
            return@withLock
        }

        val bookmark = Bookmark(
            memberId = memberId,
            postId = command.postId
        )
        val saved = bookmarkRepository.save(bookmark)
        bookmarkEventPublisher.publishBookmarked(saved)
        logger.biz.info { "북마크를 등록합니다" }
    }

    fun unbookmark(
        command: BookmarkCommand,
        memberId: Long
    ) = bookmarkLock.withLock(memberId, command.postId) {
        val bookmark = bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
            ?: return@withLock

        bookmarkRepository.delete(bookmark)
        bookmarkEventPublisher.publishUnbookmarked(bookmark)
        logger.biz.info { "북마크를 해제합니다" }
    }

    @Transactional(readOnly = true)
    fun bookmarkedPosts(memberId: Long): List<PostData> {
        val bookmarks = bookmarkRepository.findAllByMemberId(memberId)
        val postIds = bookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyList()

        return postRepository.findAllById(postIds).map(::PostData).toList()
    }
}
