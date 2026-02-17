package server.cache

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory

@Component("resilientCacheMemoryProxy")
@Primary
internal class ResilientCacheMemory(
    @param:Qualifier("redisCacheMemory")
    private val delegate: CacheMemory,
) : CacheMemory by delegate
