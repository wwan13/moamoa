package server.messaging

import org.springframework.stereotype.Component

@Component
internal class MessageHandlerInvoker {
    fun <T : Any> invoke(
        eventId: String?,
        type: String?,
        payload: T,
        handler: (T) -> Unit,
    ) {
        handler(payload)
    }
}
