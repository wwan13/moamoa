package server.messaging.health

interface MessagingHealthChecker {
    fun isHealthy(): Boolean

    fun tryRecover(): Boolean

    fun healthCheck(): Boolean {
        if (isHealthy()) return true
        return tryRecover()
    }
}
