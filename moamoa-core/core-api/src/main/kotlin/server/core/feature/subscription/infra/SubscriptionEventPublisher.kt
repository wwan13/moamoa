package server.core.feature.subscription.infra

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.infra.event.TransactionalEventPublisher

@Component
class SubscriptionEventPublisher(
    private val eventPublisher: TransactionalEventPublisher,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun publishSubscribed(subscription: Subscription) {
        eventPublisher.publish(
            TechBlogSubscribeUpdatedEvent(
                memberId = subscription.memberId,
                techBlogId = subscription.techBlogId,
                subscribed = true,
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishUnsubscribed(subscription: Subscription) {
        eventPublisher.publish(
            TechBlogSubscribeUpdatedEvent(
                memberId = subscription.memberId,
                techBlogId = subscription.techBlogId,
                subscribed = false,
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishNotificationEnabled(subscription: Subscription) {
        eventPublisher.publish(
            NotificationUpdatedEvent(
                memberId = subscription.memberId,
                techBlogId = subscription.techBlogId,
                enabled = true,
            )
        )
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun publishNotificationDisabled(subscription: Subscription) {
        eventPublisher.publish(
            NotificationUpdatedEvent(
                memberId = subscription.memberId,
                techBlogId = subscription.techBlogId,
                enabled = false,
            )
        )
    }
}
