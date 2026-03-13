package server.lock

interface KeyedLock {
    fun <T> withLock(key: String, block: () -> T): T

    fun <T> withGlobalLock(block: () -> T): T =
        withLock(GLOBAL_LOCK_KEY, block)

    private companion object {
        private const val GLOBAL_LOCK_KEY = "__globalLock"
    }
}
