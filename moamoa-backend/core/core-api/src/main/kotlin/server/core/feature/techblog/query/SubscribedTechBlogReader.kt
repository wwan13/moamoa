package server.core.feature.techblog.query

import com.linecorp.kotlinjdsl.dsl.jpql.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import server.core.feature.subscription.domain.Subscription
import server.core.global.jdsl.JdslExecutor
import server.core.feature.techblog.infra.SubscriptionCache
import server.core.infra.cache.WarmupCoordinator

@Component
class SubscribedTechBlogReader(
    private val jdslExecutor: JdslExecutor,
    private val subscriptionCache: SubscriptionCache,
    private val warmupCoordinator: WarmupCoordinator,
) {

    @Transactional(readOnly = true)
    fun findSubscribedMap(
        memberId: Long,
        techBlogIds: List<Long>
    ): Map<Long, SubscriptionInfo> {
        if (techBlogIds.isEmpty()) return emptyMap()

        val allList = loadAll(memberId)
        val allMap = allList.associateBy { it.techBlogId }

        val result = LinkedHashMap<Long, SubscriptionInfo>(techBlogIds.size)
        techBlogIds.forEach { id ->
            val info = allMap[id]
            if (info != null) result[id] = info
        }
        return result
    }

    @Transactional(readOnly = true)
    fun findAllSubscribedList(memberId: Long): List<SubscriptionInfo> {
        return loadAll(memberId)
    }

    private fun loadAll(memberId: Long): List<SubscriptionInfo> {
        subscriptionCache.get(memberId)?.let { return it }

        val subscriptions = findSubscriptionInfoList(memberId)

        val warmupKey = subscriptionCache.versionKey(memberId)
        warmupCoordinator.launchIfAbsent(warmupKey) {
            subscriptionCache.set(memberId, subscriptions)
        }

        return subscriptions
    }

    @Transactional(readOnly = true)
    fun findById(
        memberId: Long,
        techBlogId: Long
    ): SubscriptionInfo? {
        val all = loadAll(memberId)
        return all.firstOrNull { it.techBlogId == techBlogId }
    }

    private fun findSubscriptionInfoList(memberId: Long): List<SubscriptionInfo> {
        return jdslExecutor
            .createQuery(
                query = findSubscriptionInfoListQuery(memberId),
                resultClass = SubscriptionInfo::class.java,
                offset = 0,
                limit = Int.MAX_VALUE,
            )
            .resultList
    }

    private fun findSubscriptionInfoListQuery(memberId: Long) = jpql {
        selectNew<SubscriptionInfo>(
            path(Subscription::techBlogId),
            booleanLiteral(true),
            path(Subscription::notificationEnabled),
        )
            .from(
                entity(Subscription::class)
            )
            .where(
                path(Subscription::memberId).equal(memberId)
            )
    }
}
