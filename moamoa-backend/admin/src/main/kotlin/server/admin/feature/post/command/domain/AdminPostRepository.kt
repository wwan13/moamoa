package server.admin.feature.post.command.domain

import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

internal interface AdminPostRepository : JpaRepository<AdminPost, Long> {
    fun countByTechBlogId(techBlogId: Long): Long
    fun existsByTechBlogId(techBlogId: Long): Boolean
    fun findAllByTechBlogIdAndKeyIn(techBlogId: Long, keys: Collection<String>): List<AdminPost>
    
    @Query(
        """
        select p.id
        from AdminPost p
        where p.techBlogId = :techBlogId
        """
    )
    fun findIdsByTechBlogId(techBlogId: Long): List<Long>

    @Modifying
    @Query(
        """
        delete from AdminPost p
        where p.techBlogId = :techBlogId
        """
    )
    fun deleteAllByTechBlogId(techBlogId: Long): Int

    @Query(
        """
        select p.techBlogId as techBlogId, count(p.id) as count
        from AdminPost p
        where p.techBlogId in :techBlogIds
        group by p.techBlogId
        """
    )
    fun countByTechBlogIds(techBlogIds: Collection<Long>): List<AdminPostCountProjection>
}

internal interface AdminPostCountProjection {
    val techBlogId: Long
    val count: Long
}
