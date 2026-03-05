package server.core.feature.postbookmark.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.post.application.PostData
import server.core.feature.post.domain.PostRepository
import server.core.feature.postbookmark.domain.PostBookmark
import server.core.feature.postbookmark.domain.PostBookmarkRepository
import server.core.infra.db.transaction.Transactional
import server.global.logging.event

@Service
class PostBookmarkService(
    private val transactional: Transactional,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: server.lock.KeyedLock
) {
    private val logger = KotlinLogging.logger {}

    fun toggle(
        command: PostBookmarkToggleCommand,
        memberId: Long
    ): PostBookmarkToggleResult {
        val mutexKey = "postBookmarkToggle:$memberId:${command.postId}"

        return keyedLock.withLock(mutexKey) {
            transactional {
                if (!memberRepository.existsById(memberId)) {
                    throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
                }
                if (!postRepository.existsById(command.postId)) {
                    throw IllegalArgumentException("존재하지 않는 게시글 입니다.")
                }

                postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
                    ?.let { postBookmark ->
                        postBookmarkRepository.deleteById(postBookmark.id)

                        val event = postBookmark.unbookmark()
                        registerEvent(event)
                        logger.event.info(event) {
                            "게시글 북마크 해제 이벤트를 발행했습니다"
                        }

                        PostBookmarkToggleResult(false)
                    }
                    ?: let {
                        val saved = postBookmarkRepository.save(
                            PostBookmark(
                                memberId = memberId,
                                postId = command.postId
                            )
                        )

                        val event = saved.bookmark()
                        registerEvent(event)
                        logger.event.info(event) {
                            "게시글 북마크 등록 이벤트를 발행했습니다"
                        }

                        PostBookmarkToggleResult(true)
                    }
            }
        }
    }

    fun bookmarkedPosts(memberId: Long): List<PostData> {
        val postBookmarks = postBookmarkRepository.findAllByMemberId(memberId)
        val postIds = postBookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyList()

        return postRepository.findAllById(postIds).map(::PostData).toList()
    }
}
