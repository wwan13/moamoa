package server.shared.cache

import com.fasterxml.jackson.core.type.TypeReference

inline fun <reified T> CacheMemory.get(key: String): T? =
    get(key, object : TypeReference<T>() {})

inline fun <reified T> CacheMemory.mgetAs(keys: Collection<String>): Map<String, T?> =
    mgetAs(keys, object : TypeReference<T>() {})
