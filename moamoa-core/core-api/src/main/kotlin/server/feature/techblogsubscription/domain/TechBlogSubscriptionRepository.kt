package server.feature.techblogsubscription.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TechBlogSubscriptionRepository : CoroutineCrudRepository<TechBlogSubscription, Long> {
    suspend fun findByMemberIdAndTechBlogId(memberId: Long, techBlogId: Long): TechBlogSubscription?

    fun findAllByMemberId(memberId: Long): Flow<TechBlogSubscription>
}