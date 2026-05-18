package server.admin.feature.posttag.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

internal interface AdminPostTagRepository : JpaRepository<AdminPostTag, Long> {
    fun findAllByPostIdIn(postIds: Collection<Long>): List<AdminPostTag>

    @Modifying
    @Query(
        """
        delete from AdminPostTag pt
        where pt.postId in :postIds
        """
    )
    fun deleteAllByPostIdIn(postIds: Collection<Long>): Int
}
