package server.core.global.monitoring

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import server.WebhookSender
import server.content.WebhookContent
import server.core.feature.member.domain.MemberCreateEvent
import server.core.feature.submission.domain.SubmissionCreateEvent
import server.core.global.profile.isProd
import server.messaging.EventHandler
import server.messaging.invoke

@Component
class WebhookNotifier(
    private val monitoringStream: server.messaging.SubscriptionDefinition,
    private val eventHandler: EventHandler,
    private val webhookSender: WebhookSender,
    private val environment: Environment
) {

    fun memberCreateWebhookNotify() =
        eventHandler<MemberCreateEvent>(monitoringStream) { event ->
            if (!environment.isProd()) return@eventHandler

            val content = WebhookContent.Service(
                title = "회원가입",
                description = "새로운 회원가입 이벤트가 발행되었습니다.",
                fields = listOf(
                    "memberId" to event.memberId.toString(),
                    "email" to event.email,
                )
            )
            webhookSender.sendAsync(content)
        }

    fun submissionCreateWebhookNotify() =
        eventHandler<SubmissionCreateEvent>(monitoringStream) { event ->
            if (!environment.isProd()) return@eventHandler

            val content = WebhookContent.Service(
                title = "블로그 추가 요청",
                description = "새로운 블로그 추가 요청 이벤트가 발행되었습니다.",
                fields = listOf(
                    "submissionUd" to event.submissionId.toString(),
                    "blogTitle" to event.blogTitle,
                    "blogUrl" to event.blogUrl,
                )
            )
            webhookSender.sendAsync(content)
        }
}
