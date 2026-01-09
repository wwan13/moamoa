package server.feature.post.command.domain

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param

interface PostRepository : CoroutineCrudRepository<Post, Long> {
    @Modifying
    @Query(
        """
        UPDATE post
        SET bookmark_count = GREATEST(bookmark_count + :delta, 0)
        WHERE id = :postId
        """
    )
    suspend fun incrementBookmarkCount(
        @Param("postId") postId: Long,
        @Param("delta") delta: Long
    ): Int
}