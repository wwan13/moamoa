package server.feature.techblog.command.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param

interface TechBlogRepository : CoroutineCrudRepository<TechBlog, Long> {
    suspend fun findByKey(key: String): TechBlog?

    fun findAllByOrderByTitleAsc(): Flow<TechBlog>

    @Modifying
    @Query(
        """
        UPDATE tech_blog
        SET subscription_count = GREATEST(subscription_count + :delta, 0)
        WHERE id = :techBlogId
        """
    )
    suspend fun incrementSubscriptionCount(
        @Param("techBlogId") techBlogId: Long,
        @Param("delta") delta: Long
    ): Int
}