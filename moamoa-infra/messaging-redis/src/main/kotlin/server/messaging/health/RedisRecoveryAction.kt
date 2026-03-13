package server.messaging.health

internal fun interface RedisRecoveryAction {
    fun onRecovered()
}
