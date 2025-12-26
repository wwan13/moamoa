package server.domain.postbookmark

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PostBookmarkRepository : CoroutineCrudRepository<PostBookmark, Long> {
    suspend fun findByMemberIdAndPostId(memberId: Long, postId: Long): PostBookmark?
}