package server.shared.lock

interface KeyedLock {
    suspend fun <T> withLock(key: String, block: suspend () -> T): T

    suspend fun <T> withGlobalLock(block: suspend () -> T): T =
        withLock(GLOBAL_LOCK_KEY, block)

    private companion object {
        private const val GLOBAL_LOCK_KEY = "__globalLock"
    }
}
