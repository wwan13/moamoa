package server.token.jwt.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.JwtConfig
import server.token.JwtTokenProvider

@AutoConfiguration
@ConditionalOnMissingBean(name = ["jwtTokenProvider"])
@Import(
    JwtConfig::class,
    JwtTokenProvider::class,
)
class TokenJwtAutoConfiguration
