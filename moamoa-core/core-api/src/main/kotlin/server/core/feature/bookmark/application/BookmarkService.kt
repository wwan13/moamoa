package server.core.feature.bookmark.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.feature.bookmark.domain.BookmarkUpdatedEvent
import server.core.feature.member.domain.MemberRepository
import server.core.feature.post.application.PostData
import server.core.feature.post.domain.PostRepository
import server.core.global.security.UnauthorizedException
import server.core.infra.event.TransactionalEventPublisher
import server.global.logging.biz
import server.lock.KeyedLock

@Service
@Transactional
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: KeyedLock,
    private val eventPublisher: TransactionalEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun bookmark(
        command: BookmarkCommand,
        memberId: Long
    ) {
        val mutexKey = "bookmark:$memberId:${command.postId}"

        keyedLock.withLock(mutexKey) {
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
            eventPublisher.publish(
                BookmarkUpdatedEvent(
                    memberId = saved.memberId,
                    postId = saved.postId,
                    bookmarked = true,
                )
            )
            logger.biz.info { "북마크를 등록합니다" }
        }
    }

    fun unbookmark(
        command: BookmarkCommand,
        memberId: Long
    ) {
        val mutexKey = "bookmark:$memberId:${command.postId}"

        keyedLock.withLock(mutexKey) {
            val bookmark = bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
                ?: return@withLock

            bookmarkRepository.delete(bookmark)
            eventPublisher.publish(
                BookmarkUpdatedEvent(
                    memberId = bookmark.memberId,
                    postId = bookmark.postId,
                    bookmarked = false,
                )
            )
            logger.biz.info { "북마크를 해제합니다" }
        }
    }

    @Transactional(readOnly = true)
    fun bookmarkedPosts(memberId: Long): List<PostData> {
        val bookmarks = bookmarkRepository.findAllByMemberId(memberId)
        val postIds = bookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyList()

        return postRepository.findAllById(postIds).map(::PostData).toList()
    }
}
