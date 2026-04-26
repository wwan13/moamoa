package server.core.feature.subscription.domain

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByMemberIdAndTechBlogId(memberId: Long, techBlogId: Long): Subscription?

    fun findAllByMemberId(memberId: Long): List<Subscription>

    fun countByMemberId(memberId: Long): Long

    fun deleteAllByMemberId(memberId: Long): Long
}
