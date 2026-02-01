package server.feature.techblogsubscription.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.fixture.createTechBlogSubscription
import test.UnitTest

class TechBlogSubscriptionTest : UnitTest() {
    @Test
    fun `구독 이벤트는 subscribed가 true이다`() {
        val subscription = createTechBlogSubscription(
            notificationEnabled = false,
            memberId = 10L,
            techBlogId = 20L
        )

        val event = subscription.subscribe()

        event.memberId shouldBe 10L
        event.techBlogId shouldBe 20L
        event.subscribed shouldBe true
    }

    @Test
    fun `구독 해제 이벤트는 subscribed가 false이다`() {
        val subscription = createTechBlogSubscription(
            notificationEnabled = true,
            memberId = 11L,
            techBlogId = 21L
        )

        val event = subscription.unsubscribe()

        event.memberId shouldBe 11L
        event.techBlogId shouldBe 21L
        event.subscribed shouldBe false
    }

    @Test
    fun `구독 알림 토글 시 알림 상태와 이벤트가 함께 변경된다`() {
        val subscription = createTechBlogSubscription(
            notificationEnabled = true,
            memberId = 12L,
            techBlogId = 22L
        )

        val result = subscription.toggleNotification()

        result.entity.notificationEnabled shouldBe false
        val event = result.event as NotificationUpdatedEvent
        event.memberId shouldBe 12L
        event.techBlogId shouldBe 22L
        event.enabled shouldBe false
    }
}
