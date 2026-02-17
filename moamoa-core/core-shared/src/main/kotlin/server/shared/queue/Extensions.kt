package server.shared.queue

suspend inline fun <reified T> QueueMemory.lPop(key: String): T? =
    lPop(key, T::class.java)

suspend inline fun <reified T> QueueMemory.rPop(key: String): T? =
    rPop(key, T::class.java)

suspend inline fun <reified T> QueueMemory.drain(key: String, max: Int = 1000): List<T> =
    drain(key, T::class.java, max)
