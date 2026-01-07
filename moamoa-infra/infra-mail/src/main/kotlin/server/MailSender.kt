package server

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import server.config.MailProperties
import server.template.TemplateRenderer

@Component
class MailSender internal constructor(
    private val mailWebClient: WebClient,
    private val templateRenderer: TemplateRenderer,
    private val props: MailProperties
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun send(mailContent: MailContent) {
        try {
            mailWebClient.post()
                .uri("https://api.mailgun.net/v3/${props.domain}/messages")
                .headers {
                    it.setBasicAuth("api", props.apiKey)
                }
                .body(
                    BodyInserters.fromFormData("from", props.from)
                        .with("to",  mailContent.to)
                        .with("subject", mailContent.subject)
                        .apply {
                            when (mailContent) {
                                is MailContent.Text -> with("text", mailContent.text)
                                is MailContent.Html -> with("html", mailContent.text)
                                is MailContent.Template -> {
                                    val templateHtml = templateRenderer.render(mailContent.path, mailContent.args)
                                    with("html", templateHtml)
                                }
                            }
                        }
                )
                .retrieve()
                .awaitBodyOrNull<String>()
                ?: let {
                    logger.error("Could not send mail: ${mailContent.to}")
                }
        } catch (e: Exception) {
            logger.error("Could not send mail: ${mailContent.to}", e)
            throw IllegalStateException("이메일 전송에 실패하였습니다.")
        }
    }
}