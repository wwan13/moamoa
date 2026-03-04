package server.shared.set

interface SetMemory {
    fun add(key: String, value: String): Boolean

    fun members(key: String): Set<String>

    fun remove(key: String, value: String): Long
}
