package server.core.feature.subscription.infra

import org.springframework.stereotype.Component
import server.lock.KeyedLock

@Component
class SubscriptionLock(
    private val keyedLock: KeyedLock,
) {
    fun withSubscriptionLock(
        memberId: Long,
        techBlogId: Long,
        block: () -> Unit,
    ) {
        val key = "subscription:$memberId:$techBlogId"
        keyedLock.withLock(key) { block() }
    }

    fun withNotificationLock(
        memberId: Long,
        techBlogId: Long,
        block: () -> Unit,
    ) {
        val key = "notificationEnabled:$memberId:$techBlogId"
        keyedLock.withLock(key) { block() }
    }
}
