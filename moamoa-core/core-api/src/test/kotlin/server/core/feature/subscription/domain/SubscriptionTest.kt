package server.core.feature.subscription.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.core.fixture.createSubscription
import test.UnitTest

class SubscriptionTest : UnitTest() {
    @Test
    fun `구독 알림 토글 시 알림 상태가 변경된다`() {
        val subscription = createSubscription(
            notificationEnabled = true,
            memberId = 12L,
            techBlogId = 22L
        )

        subscription.toggleNotification()

        subscription.notificationEnabled shouldBe false
    }
}
