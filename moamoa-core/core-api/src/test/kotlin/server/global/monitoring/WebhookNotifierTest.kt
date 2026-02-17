package server.global.monitoring

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import server.WebhookSender
import server.content.WebhookContent
import server.feature.member.command.domain.MemberCreateEvent
import server.shared.messaging.SubscriptionDefinition
import test.UnitTest

class WebhookNotifierTest : UnitTest() {
    @Test
    fun `운영 환경에서 웹훅을 전송한다`() = runTest {
        val monitoringStream = mockk<SubscriptionDefinition>()
        val webhookSender = mockk<WebhookSender>(relaxed = true)
        val environment = mockk<Environment>()
        every { environment.activeProfiles } returns arrayOf("prod")
        val notifier = WebhookNotifier(monitoringStream, webhookSender, environment)
        val event = MemberCreateEvent(memberId = 10L, email = "moamoa@test.com")
        val expected = WebhookContent.Service(
            title = "회원가입",
            description = "새로운 회원가입 이벤트가 발행되었습니다.",
            fields = listOf(
                "memberId" to "10",
                "email" to "moamoa@test.com",
            )
        )

        val handler = notifier.memberCreateWebhookNotify()
        handler.handler(event)

        verify(exactly = 1) { webhookSender.sendAsync(expected) }
        expected shouldBe WebhookContent.Service(
            title = "회원가입",
            description = "새로운 회원가입 이벤트가 발행되었습니다.",
            fields = listOf(
                "memberId" to "10",
                "email" to "moamoa@test.com",
            )
        )
    }

    @Test
    fun `운영 환경이 아니면 웹훅을 전송하지 않는다`() = runTest {
        val monitoringStream = mockk<SubscriptionDefinition>()
        val webhookSender = mockk<WebhookSender>(relaxed = true)
        val environment = mockk<Environment>()
        every { environment.activeProfiles } returns arrayOf("local")
        val notifier = WebhookNotifier(monitoringStream, webhookSender, environment)
        val event = MemberCreateEvent(memberId = 10L, email = "moamoa@test.com")

        val handler = notifier.memberCreateWebhookNotify()
        handler.handler(event)

        verify { webhookSender wasNot Called }
    }
}
