package server.domain.postbookmark

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PostBookmarkRepository : CoroutineCrudRepository<PostBookmark, Long> {
    suspend fun findByMemberIdAndPostId(memberId: Long, postId: Long): PostBookmark?

    suspend fun findAllByMemberId(memberId: Long): Flow<PostBookmark>
}