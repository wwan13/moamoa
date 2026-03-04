package server.shared.queue

interface QueueMemory {
    fun rPush(key: String, value: Any): Long

    fun lPush(key: String, value: Any): Long

    fun rPushAll(key: String, values: Collection<Any>): Long

    fun lPushAll(key: String, values: Collection<Any>): Long

    fun <T> lPop(key: String, type: Class<T>): T?

    fun <T> rPop(key: String, type: Class<T>): T?

    fun <T> drain(key: String, type: Class<T>, max: Int = 1000): List<T>

    fun len(key: String): Long

    fun delete(key: String)
}
