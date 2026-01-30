package server.global.monitoring

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import server.WebhookSender
import server.content.WebhookContent
import server.feature.member.command.domain.MemberCreateEvent
import server.feature.submission.domain.SubmissionCreateEvent
import server.messaging.EventHandler
import server.messaging.StreamDefinition
import server.messaging.handleEvent
import support.profile.isProd

@Component
class WebhookNotifier(
    private val monitoringStream: StreamDefinition,
    private val webhookSender: WebhookSender,
    private val environment: Environment
) {

    @EventHandler
    fun memberCreateWebhookNotify() =
        handleEvent<MemberCreateEvent>(monitoringStream) { event ->
            if (!environment.isProd()) return@handleEvent

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

    @EventHandler
    fun submissionCreateWebhookNotify() =
        handleEvent<SubmissionCreateEvent>(monitoringStream) { event ->
            if (!environment.isProd()) return@handleEvent

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