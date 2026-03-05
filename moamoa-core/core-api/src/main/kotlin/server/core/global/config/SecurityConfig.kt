package server.core.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import server.core.infra.oauth2.Oauth2FailureHandler
import server.core.infra.oauth2.Oauth2SuccessHandler
import server.core.infra.oauth2.Oauth2UserService

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oauth2SuccessHandler: Oauth2SuccessHandler,
    private val oauth2FailureHandler: Oauth2FailureHandler,
    private val oauth2UserService: Oauth2UserService,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .redirectionEndpoint { it.baseUri("/auth/oauth2/callback/*") }
                    .userInfoEndpoint { it.userService(oauth2UserService) }
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            }
            .build()
    }
}
