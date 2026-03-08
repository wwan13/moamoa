package server.core.feature.subscription.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.core.fixture.createSubscription
import test.UnitTest

class SubscriptionTest : UnitTest() {
    @Test
    fun `구독 이벤트는 subscribed가 true이다`() {
        val subscription = createSubscription(
            notificationEnabled = false,
            memberId = 10L,
            techBlogId = 20L
        )

        subscription.subscribe()
        val event = extractSingleEvent(subscription) as TechBlogSubscribeUpdatedEvent

        event.memberId shouldBe 10L
        event.techBlogId shouldBe 20L
        event.subscribed shouldBe true
    }

    @Test
    fun `구독 해제 이벤트는 subscribed가 false이다`() {
        val subscription = createSubscription(
            notificationEnabled = true,
            memberId = 11L,
            techBlogId = 21L
        )

        subscription.unsubscribe()
        val event = extractSingleEvent(subscription) as TechBlogSubscribeUpdatedEvent

        event.memberId shouldBe 11L
        event.techBlogId shouldBe 21L
        event.subscribed shouldBe false
    }

    @Test
    fun `구독 알림 토글 시 알림 상태와 이벤트가 함께 변경된다`() {
        val subscription = createSubscription(
            notificationEnabled = true,
            memberId = 12L,
            techBlogId = 22L
        )

        subscription.toggleNotification()
        val event = extractSingleEvent(subscription) as NotificationUpdatedEvent

        subscription.notificationEnabled shouldBe false
        event.memberId shouldBe 12L
        event.techBlogId shouldBe 22L
        event.enabled shouldBe false
    }

    private fun extractSingleEvent(entity: Any): Any {
        var type: Class<*>? = entity.javaClass
        while (type != null) {
            runCatching {
                val field = type.getDeclaredField("domainEvents")
                field.isAccessible = true
                val events = field.get(entity) as MutableCollection<*>
                return events.single()!!
            }
            type = type.superclass
        }
        error("domainEvents field not found")
    }
}
