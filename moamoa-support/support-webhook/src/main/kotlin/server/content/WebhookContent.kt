package server.content

sealed class WebhookContent{
    abstract val title: String
    abstract val description: String
    abstract val fields: List<Pair<String, String>>

    data class Error(
        override val title: String,
        override val description: String,
        override val fields: List<Pair<String, String>>,
    ) : WebhookContent()

    data class Service(
        override val title: String,
        override val description: String,
        override val fields: List<Pair<String, String>>,
    ) : WebhookContent()

    data class Batch(
        override val title: String,
        override val description: String,
        override val fields: List<Pair<String, String>>,
    ) : WebhookContent()
}