package server.mail.mailgun.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.MailConfig
import server.config.MailTemplateConfig
import server.mail.MailgunMailSender
import server.mail.template.TemplateRenderer

@AutoConfiguration
@ConditionalOnMissingBean(name = ["mailgunMailSender"])
@Import(
    MailConfig::class,
    MailTemplateConfig::class,
    TemplateRenderer::class,
    MailgunMailSender::class,
)
class MailMailgunAutoConfiguration
