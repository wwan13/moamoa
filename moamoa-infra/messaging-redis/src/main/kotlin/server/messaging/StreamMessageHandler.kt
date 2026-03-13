package server.messaging

import server.messaging.definition.EventStream

internal data class StreamMessageHandler(
    val stream: EventStream,
    val type: String,
    val payloadClass: Class<out Any>,
    val handler: (Any) -> Unit,
)