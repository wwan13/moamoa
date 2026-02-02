package server.infra.oauth2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import reactor.core.publisher.Mono
import server.feature.member.command.application.CreateSocialMemberCommand
import server.feature.member.command.application.MemberData
import server.feature.member.command.application.MemberService
import server.feature.member.command.domain.MemberRole
import server.feature.member.command.domain.Provider
import test.UnitTest
import java.time.Instant

class Oauth2UserServiceTest : UnitTest() {
    @Test
    fun `기존 소셜 회원이면 Authenticated를 반환한다`() {
        val memberService = mockk<MemberService>()
        val service = Oauth2UserService(memberService)
        val delegate = mockk<DefaultReactiveOAuth2UserService>()
        val userRequest = userRequest("google")
        val oauth2User = oauthUser(mapOf("sub" to "google-1", "email" to "a@b.com"))

        setDelegate(service, delegate)
        every { delegate.loadUser(userRequest) } returns Mono.just(oauth2User)
        coEvery {
            memberService.findSocialMember(Provider.GOOGLE, "google-1")
        } returns MemberData(
            id = 10L,
            email = "a@b.com",
            role = MemberRole.USER
        )

        val result = service.loadUser(userRequest).block()

        result shouldBe Oauth2SocialUser.Authenticated(
            memberId = 10L,
            role = MemberRole.USER,
            isNew = false
        )
    }

    @Test
    fun `이메일이 없으면 EmailRequired를 반환한다`() {
        val memberService = mockk<MemberService>()
        val service = Oauth2UserService(memberService)
        val delegate = mockk<DefaultReactiveOAuth2UserService>()
        val userRequest = userRequest("github")
        val oauth2User = oauthUser(mapOf("id" to 1234))

        setDelegate(service, delegate)
        every { delegate.loadUser(userRequest) } returns Mono.just(oauth2User)
        coEvery {
            memberService.findSocialMember(Provider.GITHUB, "1234")
        } throws IllegalArgumentException("not found")

        val result = service.loadUser(userRequest).block()

        result shouldBe Oauth2SocialUser.EmailRequired(
            provider = Provider.GITHUB,
            providerKey = "1234"
        )
    }

    @Test
    fun `없던 회원이고 이메일이 있으면 생성 후 Authenticated를 반환한다`() {
        val memberService = mockk<MemberService>()
        val service = Oauth2UserService(memberService)
        val delegate = mockk<DefaultReactiveOAuth2UserService>()
        val userRequest = userRequest("google")
        val oauth2User = oauthUser(mapOf("sub" to "google-2", "email" to "new@b.com"))

        setDelegate(service, delegate)
        every { delegate.loadUser(userRequest) } returns Mono.just(oauth2User)
        coEvery {
            memberService.findSocialMember(Provider.GOOGLE, "google-2")
        } throws IllegalArgumentException("not found")
        coEvery {
            memberService.createSocialMember(
                CreateSocialMemberCommand(
                    email = "new@b.com",
                    provider = Provider.GOOGLE,
                    providerKey = "google-2"
                )
            )
        } returns MemberData(
            id = 20L,
            email = "new@b.com",
            role = MemberRole.USER
        )

        val result = service.loadUser(userRequest).block()

        result shouldBe Oauth2SocialUser.Authenticated(
            memberId = 20L,
            role = MemberRole.USER,
            isNew = true
        )
    }

    @Test
    fun `회원 생성 중 예외가 발생하면 HasError를 반환한다`() {
        val memberService = mockk<MemberService>()
        val service = Oauth2UserService(memberService)
        val delegate = mockk<DefaultReactiveOAuth2UserService>()
        val userRequest = userRequest("google")
        val oauth2User = oauthUser(mapOf("sub" to "google-3", "email" to "err@b.com"))

        setDelegate(service, delegate)
        every { delegate.loadUser(userRequest) } returns Mono.just(oauth2User)
        coEvery {
            memberService.findSocialMember(Provider.GOOGLE, "google-3")
        } throws IllegalArgumentException("not found")
        coEvery {
            memberService.createSocialMember(
                CreateSocialMemberCommand(
                    email = "err@b.com",
                    provider = Provider.GOOGLE,
                    providerKey = "google-3"
                )
            )
        } throws IllegalStateException("boom")

        val result = service.loadUser(userRequest).block()

        result shouldBe Oauth2SocialUser.HasError(message = "boom")
    }

    @Test
    fun `지원하지 않는 provider 이면 예외를 반환한다`() {
        val memberService = mockk<MemberService>()
        val service = Oauth2UserService(memberService)
        val delegate = mockk<DefaultReactiveOAuth2UserService>()
        val userRequest = userRequest("naver")
        val oauth2User = oauthUser(mapOf("id" to "x"))

        setDelegate(service, delegate)
        every { delegate.loadUser(userRequest) } returns Mono.just(oauth2User)

        shouldThrow<IllegalStateException> {
            service.loadUser(userRequest).block()
        }
    }

    private fun oauthUser(attributes: Map<String, Any?>): OAuth2User {
        val oauth2User = mockk<OAuth2User>()
        io.mockk.every { oauth2User.attributes } returns attributes
        return oauth2User
    }

    private fun setDelegate(service: Oauth2UserService, delegate: DefaultReactiveOAuth2UserService) {
        val field = Oauth2UserService::class.java.getDeclaredField("delegate")
        field.isAccessible = true
        field.set(service, delegate)
    }

    private fun userRequest(registrationId: String): OAuth2UserRequest {
        val registration = ClientRegistration.withRegistrationId(registrationId)
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/$registrationId")
            .scope("profile")
            .authorizationUri("http://auth.example.com")
            .tokenUri("http://token.example.com")
            .userInfoUri("http://user.example.com")
            .userNameAttributeName("id")
            .clientName(registrationId)
            .build()

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60)
        )
        return OAuth2UserRequest(registration, accessToken)
    }
}
