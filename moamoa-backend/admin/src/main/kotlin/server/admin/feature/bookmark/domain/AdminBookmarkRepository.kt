package server.admin.feature.bookmark.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

internal interface AdminBookmarkRepository : JpaRepository<AdminBookmark, Long> {
    @Modifying
    @Query(
        """
        delete from AdminBookmark b
        where b.postId in :postIds
        """
    )
    fun deleteAllByPostIdIn(postIds: Collection<Long>): Int
}
