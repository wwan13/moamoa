package server.infra.oauth2

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import server.feature.member.command.domain.MemberRole
import server.feature.member.command.domain.Provider
import test.UnitTest

class Oauth2SocialUserTest : UnitTest() {
    @Test
    fun `Authenticated 는 속성이 없다`() {
        val user = Oauth2SocialUser.Authenticated(
            memberId = 10L,
            role = MemberRole.USER,
            isNew = false
        )

        user.attributes.shouldBeEmpty()
    }

    @Test
    fun `EmailRequired 는 속성이 없다`() {
        val user = Oauth2SocialUser.EmailRequired(
            provider = Provider.GOOGLE,
            providerKey = "google-123"
        )

        user.attributes.shouldBeEmpty()
    }

    @Test
    fun `HasError 는 속성이 없다`() {
        val user = Oauth2SocialUser.HasError(message = "boom")

        user.attributes.shouldBeEmpty()
    }

    @Test
    fun `권한은 비어있다`() {
        val user = Oauth2SocialUser.HasError(message = "boom")

        user.authorities.shouldBeEmpty()
    }

    @Test
    fun `이름은 비어있지 않다`() {
        val user = Oauth2SocialUser.HasError(message = "boom")

        user.name.shouldNotBeBlank()
    }
}
