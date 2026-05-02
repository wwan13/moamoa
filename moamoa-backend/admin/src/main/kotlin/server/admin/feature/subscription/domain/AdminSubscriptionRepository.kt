package server.admin.feature.subscription.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

internal interface AdminSubscriptionRepository : JpaRepository<AdminSubscription, Long> {
    @Query(
        """
        select s.techBlogId as techBlogId, count(s.id) as count
        from AdminSubscription s
        where s.techBlogId in :techBlogIds
        group by s.techBlogId
        """
    )
    fun countByTechBlogIds(techBlogIds: Collection<Long>): List<AdminSubscriptionCountProjection>
}

internal interface AdminSubscriptionCountProjection {
    val techBlogId: Long
    val count: Long
}
