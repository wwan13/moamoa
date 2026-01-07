package server.chat

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import server.config.AiProperties

@Component
class ChatCompletion internal constructor(
    private val props: AiProperties,
    private val webClient: WebClient
) {
    suspend fun invoke(vararg prompts: Prompt): String {
        val request = Request(
            model = props.model,
            messages = prompts.map {
                ChatMessage(
                    role = it.role,
                    content = it.message
                )
            }
        )

        val response = webClient.post()
            .uri("${props.baseUrl}/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${props.secretKey}"
            )
            .bodyValue(request)
            .retrieve()
            .bodyToMono<ChatCompletionResponse>()
            .awaitSingle()

        return response.firstMessage()
    }

    private data class Request(
        val model: String,
        val messages: List<ChatMessage>,
    )

    private data class ChatCompletionResponse(
        val choices: List<ChatChoice>,
    ) {
        data class ChatChoice(
            val index: Int,
            val message: ChatMessage
        )

        fun firstMessage(): String =
            choices.firstOrNull()?.message?.content ?: ""
    }

    private data class ChatMessage(
        val role: String,
        val content: String
    )
}