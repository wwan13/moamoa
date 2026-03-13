package server.messaging.definition

enum class EventChannel(
    val key: String,
) {
    DEFAULT("moamoa-default"),
    DEFAULT_DLQ("moamoa-default-dlq"),
}
