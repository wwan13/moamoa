package server.infra.oauth2

import kotlinx.coroutines.reactor.mono
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import server.feature.member.command.application.CreateSocialMemberCommand
import server.feature.member.command.application.MemberService
import server.feature.member.command.domain.Provider

@Service
class Oauth2UserService (
    private val memberService: MemberService
): ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val delegate = DefaultReactiveOAuth2UserService()

    override fun loadUser(userRequest: OAuth2UserRequest): Mono<OAuth2User> {
        return delegate.loadUser(userRequest)
            .flatMap { oAuth2User ->
                val registrationId = userRequest.clientRegistration.registrationId
                val attributes = oAuth2User.attributes

                val provider = Provider.from(registrationId)

                val oauthAttributes = when (provider) {
                    Provider.GOOGLE -> fromGoogle(attributes)
                    Provider.GITHUB -> fromGithub(attributes)
                    else -> {
                        return@flatMap Mono.error<OAuth2User>(
                            IllegalArgumentException("Not Oauth provider")
                        )
                    }
                }

                mono {
                    runCatching {
                        val member = memberService.findSocialMember(
                            provider = oauthAttributes.provider,
                            providerKey = oauthAttributes.providerKey,
                        )

                        Oauth2SocialUser.Authenticated(
                            memberId = member.id,
                            role = member.role,
                            isNew = false
                        )
                    }.getOrElse {
                        if (oauthAttributes.email == null) {
                             return@mono Oauth2SocialUser.EmailRequired(
                                 provider = oauthAttributes.provider,
                                 providerKey = oauthAttributes.providerKey
                             )
                        }

                        val command = CreateSocialMemberCommand(
                            email = oauthAttributes.email,
                            provider = oauthAttributes.provider,
                            providerKey = oauthAttributes.providerKey,
                        )
                        try {
                            val member = memberService.createSocialMember(command)
                            Oauth2SocialUser.Authenticated(
                                memberId = member.id,
                                role = member.role,
                                isNew = true
                            )
                        } catch (e: Exception) {
                            return@mono Oauth2SocialUser.HasError(
                                message = e.message ?: "로그인 중 오류가 발생했습니다. 다시 시도해 주세요.",
                            )
                        }
                    }
                }
            }
    }

    private fun fromGoogle(attr: Map<String, Any?>) = OauthAttributes(
        provider = Provider.GOOGLE,
        providerKey = attr["sub"]?.toString()
            ?: throw IllegalStateException("Invalid naver login response format"),
        email = attr["email"]?.toString()
            ?: throw IllegalStateException("Invalid naver login response format"),
    )

    private fun fromGithub(attr: Map<String, Any?>): OauthAttributes {
        val id = attr["id"]?.toString()
            ?: throw IllegalStateException("GitHub id not found")

        val email = attr["email"] as? String

        return OauthAttributes(
            provider = Provider.GITHUB,
            providerKey = id,
            email = email
        )
    }

    private data class OauthAttributes(
        val provider: Provider,
        val providerKey: String,
        val email: String?
    )
}
