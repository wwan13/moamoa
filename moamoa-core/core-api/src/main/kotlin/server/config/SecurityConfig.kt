package server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import server.infra.oauth2.Oauth2FailureHandler
import server.infra.oauth2.Oauth2SuccessHandler

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val oauth2SuccessHandler: Oauth2SuccessHandler,
    private val oauth2FailureHandler: Oauth2FailureHandler,
) {

    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.authenticationMatcher(
                    PathPatternParserServerWebExchangeMatcher("/auth/oauth2/callback/{registrationId}")
                )
                oauth2.authenticationSuccessHandler(oauth2SuccessHandler)
                oauth2.authenticationFailureHandler(oauth2FailureHandler)
            }
            .build()
    }
}