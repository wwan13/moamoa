package server.messaging

import org.springframework.stereotype.Component

@Component
internal class StreamEventHandlers(
    private val handlers: List<StreamEventHandler<*>>
) {
    private data class Key(val streamKey: String, val group: String, val type: String)

    private val map: Map<Key, StreamEventHandler<*>> =
        handlers.associateBy { Key(it.stream.topic.key, it.stream.group, it.type) }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> find(stream: StreamDefinition, type: String): StreamEventHandler<T>? =
        map[Key(stream.topic.key, stream.group, type)] as? StreamEventHandler<T>

    fun streams(): List<StreamDefinition> =
        handlers
            .map { it.stream }
            .distinctBy { it.topic to it.group }
            .toList()
}