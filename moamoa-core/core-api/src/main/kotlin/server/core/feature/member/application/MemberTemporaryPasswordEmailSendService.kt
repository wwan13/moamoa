package server.core.feature.member.application

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import server.core.feature.member.application.ApplyTemporaryPasswordEvent
import server.mail.MailContent
import server.mail.MailSender
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream
import server.template.mail.MailTemplate
import server.template.mail.toTemplateArgs

@Service
class MemberTemporaryPasswordEmailSendService(
    private val mailSender: MailSender,
) {

    @EventHandler(EventStream.DEFAULT)
    fun sendEmail(event: ApplyTemporaryPasswordEvent) = runBlocking {
        val template = MailTemplate.ApplyTemporaryPassword(event.temporaryPassword)
        val content = MailContent.Template(
            event.email,
            MAIL_SUBJECT,
            template.path,
            template.toTemplateArgs()
        )
        mailSender.send(content)
    }

    companion object {
        private const val MAIL_SUBJECT = "[모아모아] 임시 비밀번호 안내입니다."
    }
}
