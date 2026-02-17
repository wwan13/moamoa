package server.cache

import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import server.config.ResilientCacheProperties
import server.shared.cache.CacheMemory

class ResilientCacheMemoryContextTest {
    @Test
    fun `CacheMemory 주입 시 ResilientCacheMemory가 선택된다`() {
        AnnotationConfigApplicationContext(TestConfig::class.java).use { context ->
            val cacheMemory = context.getBean(CacheMemory::class.java)

            cacheMemory.shouldBeInstanceOf<ResilientCacheMemory>()
        }
    }

    @Configuration
    @Import(ResilientCacheMemory::class)
    internal class TestConfig {
        @Bean("redisCacheMemory")
        fun redisCacheMemory(): CacheMemory = mockk(relaxed = true)

        @Bean("caffeineCacheMemory")
        fun caffeineCacheMemory(): CacheMemory = mockk(relaxed = true)

        @Bean
        fun resilientCacheProperties(): ResilientCacheProperties = ResilientCacheProperties()
    }
}
