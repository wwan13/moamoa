package server.messaging

import org.springframework.stereotype.Component

@Component
class EventHandler(
) : AbstractEventHandler() {
    override fun <T : Any> wrap(handler: (T) -> Unit): (T) -> Unit = handler
}
