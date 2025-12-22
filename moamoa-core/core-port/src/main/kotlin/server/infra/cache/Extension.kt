package server.infra.cache

suspend inline fun <reified T> CacheMemory.get(key: String): T? {
    return get(key, T::class.java)
}