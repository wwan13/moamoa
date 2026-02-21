package server.messaging.health

internal fun interface RedisRecoveryAction {
    suspend fun onRecovered()
}
