package server.messaging

import org.springframework.stereotype.Component

@Component
internal class StreamEventHandlers(
    private val handlers: List<StreamEventHandler<*>>
) {
    private val map = handlers.associateBy { it.streamKey to it.type }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> find(streamKey: String, type: String): StreamEventHandler<T>? =
        map[streamKey to type] as? StreamEventHandler<T>

    fun streamKeys(): List<String> =
        handlers
            .map { it.streamKey }
            .distinct()
            .toList()
}