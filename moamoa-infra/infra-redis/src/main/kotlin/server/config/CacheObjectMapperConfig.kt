package server.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class CacheObjectMapperConfig() {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper::class)
    fun objectMapper() = jacksonObjectMapper()
}