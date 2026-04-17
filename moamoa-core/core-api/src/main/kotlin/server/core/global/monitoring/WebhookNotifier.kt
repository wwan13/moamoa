package server.core.global.monitoring

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import server.WebhookSender
import server.content.WebhookContent
import server.core.feature.feedback.domain.FeedbackCreateEvent
import server.core.feature.member.domain.MemberCreateEvent
import server.core.feature.submission.domain.SubmissionCreateEvent
import server.core.global.profile.isProd
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream

@Component
class WebhookNotifier(
    private val webhookSender: WebhookSender,
    private val environment: Environment
) {

    @EventHandler(stream = EventStream.MONITORING)
    fun memberCreateWebhookNotify(event: MemberCreateEvent) {
        if (!environment.isProd()) return

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

    @EventHandler(stream = EventStream.MONITORING)
    fun submissionCreateWebhookNotify(event: SubmissionCreateEvent) {
        if (!environment.isProd()) return

        val content = WebhookContent.Service(
            title = "블로그 추가 요청",
            description = "새로운 블로그 추가 요청 이벤트가 발행되었습니다.",
            fields = listOf(
                "submissionId" to event.submissionId.toString(),
                "blogTitle" to event.blogTitle,
                "blogUrl" to event.blogUrl,
            )
        )
        webhookSender.sendAsync(content)
    }

    @EventHandler(stream = EventStream.MONITORING)
    fun feedbackCreateWebhookNotify(event: FeedbackCreateEvent) {
//        if (!environment.isProd()) return

        val content = WebhookContent.Service(
            title = "피드백 도착",
            description = "사용자로부터 피드백이 도착했스니다.",
            fields = listOf(
                "feedbackId" to event.feedbackId.toString(),
                "type" to event.feedbackType.name,
                "content" to event.content,
                "email" to event.email,
            )
        )
        webhookSender.sendAsync(content)
    }
}
