package server.shared.cache

import com.fasterxml.jackson.core.type.TypeReference

suspend inline fun <reified T> CacheMemory.get(key: String): T? =
    get(key, object : TypeReference<T>() {})

suspend inline fun <reified T> CacheMemory.mgetAs(keys: Collection<String>): Map<String, T?> =
    mgetAs(keys, object : TypeReference<T>() {})
