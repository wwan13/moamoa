package server.core.feature.bookmark.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.post.application.PostData
import server.core.feature.post.domain.PostRepository
import server.core.feature.bookmark.domain.Bookmark
import server.core.feature.bookmark.domain.BookmarkRepository
import server.core.infra.db.transaction.Transactional
import server.global.logging.event
import server.lock.KeyedLock

@Service
class BookmarkService(
    private val transactional: Transactional,
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: KeyedLock
) {
    private val logger = KotlinLogging.logger {}

    fun toggle(
        command: BookmarkToggleCommand,
        memberId: Long
    ): BookmarkToggleResult {
        val mutexKey = "bookmarkToggle:$memberId:${command.postId}"

        return keyedLock.withLock(mutexKey) {
            transactional {
                if (!memberRepository.existsById(memberId)) {
                    throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
                }
                if (!postRepository.existsById(command.postId)) {
                    throw IllegalArgumentException("존재하지 않는 게시글 입니다.")
                }

                bookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
                    ?.let { bookmark ->
                        bookmarkRepository.deleteById(bookmark.id)

                        val event = bookmark.unbookmark()
                        registerEvent(event)
                        logger.event.info(event) {
                            "게시글 북마크 해제 이벤트를 발행했습니다"
                        }

                        BookmarkToggleResult(false)
                    }
                    ?: let {
                        val saved = bookmarkRepository.save(
                            Bookmark(
                                memberId = memberId,
                                postId = command.postId
                            )
                        )

                        val event = saved.bookmark()
                        registerEvent(event)
                        logger.event.info(event) {
                            "게시글 북마크 등록 이벤트를 발행했습니다"
                        }

                        BookmarkToggleResult(true)
                    }
            }
        }
    }

    fun bookmarkedPosts(memberId: Long): List<PostData> {
        val bookmarks = bookmarkRepository.findAllByMemberId(memberId)
        val postIds = bookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyList()

        return postRepository.findAllById(postIds).map(::PostData).toList()
    }
}
