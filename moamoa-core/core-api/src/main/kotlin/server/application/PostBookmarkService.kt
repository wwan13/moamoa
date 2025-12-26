package server.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.domain.member.MemberRepository
import server.domain.post.PostRepository
import server.domain.postbookmark.PostBookmarkRepository

@Service
class PostBookmarkService(
    private val postBookmarkRepository: PostBookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository
) {

    suspend fun toggle(command: PostBookmarkToggleCommand, memberId: Long): PostBookmarkToggleResult {
        if (!memberRepository.existsById(memberId)) {
            throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
        }
        if (!postRepository.existsById(command.postId)) {
            throw IllegalArgumentException("존재하지 않는 게시글 입니다.")
        }

        val bookmarked = postBookmarkRepository.findByMemberIdAndPostId(memberId, command.postId)
            ?.let { postBookmark ->
                postBookmarkRepository.deleteById(postBookmark.id)
                false
            }
            ?: run {
                val postBookmark = server.domain.postbookmark.PostBookmark(
                    memberId = memberId,
                    postId = command.postId
                )
                postBookmarkRepository.save(postBookmark)
                true
            }

        return PostBookmarkToggleResult(bookmarked)
    }

    suspend fun bookmarkedPosts(memberId: Long): Flow<PostData> {
        val postBookmarks = postBookmarkRepository.findAllByMemberId(memberId).toList()
        val postIds = postBookmarks.map { it.postId }
        if (postIds.isEmpty()) return emptyFlow()

        return postRepository.findAllById(postIds).map(::PostData)
    }
}