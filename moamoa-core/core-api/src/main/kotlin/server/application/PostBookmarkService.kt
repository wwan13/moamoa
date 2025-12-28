package server.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import server.domain.member.MemberRepository
import server.domain.post.PostRepository
import server.domain.postbookmark.PostBookmarkCreatedEvent
import server.domain.postbookmark.PostBookmarkRemovedEvent
import server.domain.postbookmark.PostBookmarkRepository
import server.infra.db.Transactional
import server.infra.event.EventPublisher

@Service
class PostBookmarkService(
    private val transactional: Transactional,
    private val postBookmarkRepository: PostBookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: EventPublisher
) {

    suspend fun toggle(
        command: PostBookmarkToggleCommand,
        memberId: Long
    ): PostBookmarkToggleResult = transactional {
        if (!memberRepository.existsById(memberId)) {
            throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
        }
        if (!postRepository.existsById(command.postId)) {
            throw IllegalArgumentException("존재하지 않는 게시글 입니다.")
        }

        val bookmarked = postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
            ?.let { postBookmark ->
                postBookmarkRepository.deleteById(postBookmark.id)
                eventPublisher.publish(PostBookmarkRemovedEvent(command.postId))
                false
            }
            ?: run {
                val postBookmark = server.domain.postbookmark.PostBookmark(
                    memberId = memberId,
                    postId = command.postId
                )
                postBookmarkRepository.save(postBookmark)
                eventPublisher.publish(PostBookmarkCreatedEvent(command.postId))
                true
            }

        PostBookmarkToggleResult(bookmarked)
    }

    suspend fun bookmarkedPosts(memberId: Long): Flow<PostData> {
        val postBookmarks = postBookmarkRepository.findAllByMemberId(memberId).toList()
        val postIds = postBookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyFlow()

        return postRepository.findAllById(postIds).map(::PostData)
    }
}