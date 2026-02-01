package server.feature.postbookmark.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.feature.post.command.application.PostData
import server.feature.member.command.domain.MemberRepository
import server.feature.post.command.domain.PostRepository
import server.feature.postbookmark.domain.PostBookmarkUpdatedEvent
import server.feature.postbookmark.domain.PostBookmarkRepository
import server.feature.postbookmark.domain.PostBookmark
import server.global.lock.KeyedMutex
import server.infra.db.transaction.Transactional
import server.messaging.StreamEventPublisher
import server.messaging.StreamTopic

@Service
class PostBookmarkService(
    private val transactional: Transactional,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: StreamEventPublisher,
    private val defaultTopic: StreamTopic,
    private val keyedMutex: KeyedMutex
) {

    suspend fun toggle(
        command: PostBookmarkToggleCommand,
        memberId: Long
    ): PostBookmarkToggleResult {
        val mutexKey = "postBookmarkToggle:$memberId:${command.postId}"

        return keyedMutex.withLock(mutexKey) {
            val bookmarked = transactional {
                if (!memberRepository.existsById(memberId)) {
                    throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
                }
                if (!postRepository.existsById(command.postId)) {
                    throw IllegalArgumentException("존재하지 않는 게시글 입니다.")
                }

                postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
                    ?.let { postBookmark ->
                        postBookmarkRepository.deleteById(postBookmark.id)
                        false
                    }
                    ?: run {
                        postBookmarkRepository.save(
                            PostBookmark(
                                memberId = memberId,
                                postId = command.postId
                            )
                        )
                        true
                    }
            }

            eventPublisher.publish(
                defaultTopic,
                PostBookmarkUpdatedEvent(memberId, command.postId, bookmarked)
            )

            PostBookmarkToggleResult(bookmarked)
        }
    }

    suspend fun bookmarkedPosts(memberId: Long): Flow<PostData> {
        val postBookmarks = postBookmarkRepository.findAllByMemberId(memberId).toList()
        val postIds = postBookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyFlow()

        return postRepository.findAllById(postIds).map(::PostData)
    }
}