package server.messaging

interface Event {
    val type: String
        get() = this::class.simpleName!!
}
