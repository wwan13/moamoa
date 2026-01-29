package server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import server.config.WebhookProperties
import server.content.WebhookContent

@Component
class WebhookSender internal constructor(
    private val webhookWebClient: WebClient,
    private val props: WebhookProperties,
    private val webhookScope: CoroutineScope
) {

    suspend fun sendAsync(content: WebhookContent) =
        webhookScope.launch {
            send(content)
        }

    suspend fun send(content: WebhookContent) {
        val (url, color) = when (content) {
            is WebhookContent.Batch -> props.batch to GREY
            is WebhookContent.Error -> props.error to RED
            is WebhookContent.Service -> props.service to GREEN
        }

        val request = Request(
            embeds = listOf(
                Request.Embed(
                    title = content.title,
                    description = content.description,
                    color = color,
                    fields = content.fields.map {
                        Request.Embed.EmbedField(
                            name = it.first,
                            value = it.second
                        )
                    }
                )
            )
        )

        webhookWebClient.post()
            .uri(url)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<Void>()
            .awaitSingleOrNull()
    }

    private data class Request(
        val embeds: List<Embed>
    ) {
        data class Embed(
            val title: String,
            val description: String,
            val fields: List<EmbedField>,
            val color: Int,
        ) {
            data class EmbedField(
                val name: String,
                val value: String,
                val inline: Boolean = false
            )
        }
    }

    companion object {
        const val GREEN = 0x57F287
        const val RED = 0xED4245
        const val GREY = 0x99AAB5
    }
}