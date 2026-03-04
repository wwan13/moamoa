package server.feature.techblogsubscription.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TechBlogSubscriptionRepository : JpaRepository<TechBlogSubscription, Long> {
    fun findByMemberIdAndTechBlogId(memberId: Long, techBlogId: Long): TechBlogSubscription?

    fun findAllByMemberId(memberId: Long): List<TechBlogSubscription>

    fun countByMemberId(memberId: Long): Long

    fun deleteAllByMemberId(memberId: Long): Long
}
