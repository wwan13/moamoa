package server.core.feature.subscription.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.core.fixture.createSubscription
import test.UnitTest

class SubscriptionTest : UnitTest() {
    @Test
    fun `구독 알림 활성화 시 알림 상태가 true가 된다`() {
        val subscription = createSubscription(
            notificationEnabled = false,
            memberId = 12L,
            techBlogId = 22L
        )

        subscription.enableNotification()

        subscription.notificationEnabled shouldBe true
    }

    @Test
    fun `구독 알림 비활성화 시 알림 상태가 false가 된다`() {
        val subscription = createSubscription(
            notificationEnabled = true,
            memberId = 12L,
            techBlogId = 22L
        )

        subscription.disableNotification()

        subscription.notificationEnabled shouldBe false
    }
}
