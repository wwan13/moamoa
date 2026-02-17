package server.shared.queue

interface QueueMemory {
    suspend fun rPush(key: String, value: Any): Long

    suspend fun lPush(key: String, value: Any): Long

    suspend fun rPushAll(key: String, values: Collection<Any>): Long

    suspend fun lPushAll(key: String, values: Collection<Any>): Long

    suspend fun <T> lPop(key: String, type: Class<T>): T?

    suspend fun <T> rPop(key: String, type: Class<T>): T?

    suspend fun <T> drain(key: String, type: Class<T>, max: Int = 1000): List<T>

    suspend fun len(key: String): Long

    suspend fun delete(key: String)
}
