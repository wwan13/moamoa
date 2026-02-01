package server.feature.member.command.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.fixture.createMember
import test.UnitTest

class MemberTest : UnitTest() {
    @Test
    fun `멤버 생성 이벤트는 멤버의 아이디와 이메일을 담는다`() {
        val member = createMember(id = 10L, email = "user@example.com")

        val event = member.created()

        event shouldBe MemberCreateEvent(
            memberId = member.id,
            email = member.email
        )
    }

    @Test
    fun `내부 회원 생성 시 INTERNAL provider와 빈 providerKey를 가진다`() {
        val member = Member.fromInternal(
            email = "user@example.com",
            password = "encoded-password"
        )

        member.email shouldBe "user@example.com"
        member.password shouldBe "encoded-password"
        member.provider shouldBe Provider.INTERNAL
        member.providerKey shouldBe ""
    }

    @Test
    fun `소셜 회원 생성 시 provider가 INTERNAL이면 예외가 발생한다`() {
        val exception = shouldThrow<IllegalStateException> {
            Member.fromSocial(
                email = "user@example.com",
                provider = Provider.INTERNAL,
                providerKey = "internal-key"
            )
        }

        exception.message shouldBe "소셜 로그인으로 회원가입한 유저입니다."
    }

    @Test
    fun `소셜 회원 생성 시 provider와 providerKey를 저장하고 비밀번호는 비어있다`() {
        val member = Member.fromSocial(
            email = "social@example.com",
            provider = Provider.GITHUB,
            providerKey = "github-key"
        )

        member.email shouldBe "social@example.com"
        member.provider shouldBe Provider.GITHUB
        member.providerKey shouldBe "github-key"
        member.password shouldBe ""
    }
}
