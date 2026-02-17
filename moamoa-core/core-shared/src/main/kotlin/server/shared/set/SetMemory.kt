package server.shared.set

interface SetMemory {
    suspend fun add(key: String, value: String): Boolean

    suspend fun members(key: String): Set<String>

    suspend fun remove(key: String, value: String): Long
}
