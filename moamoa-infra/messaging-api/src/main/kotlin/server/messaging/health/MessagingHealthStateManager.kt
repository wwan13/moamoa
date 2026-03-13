package server.messaging.health

interface MessagingHealthStateManager {
    fun <T> runSafe(block: () -> T): Result<T>

    fun isDegraded(): Boolean

    fun isFailure(exception: Throwable): Boolean

    fun tryRecover(): Boolean
}
