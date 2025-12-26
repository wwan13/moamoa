package server.domain.techblogsubscription

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TechBlogSubscriptionRepository : CoroutineCrudRepository<TechBlogSubscription, Long> {
    suspend fun findByMemberIdAndTechBlogId(memberId: Long, techBlogId: Long): TechBlogSubscription?
}