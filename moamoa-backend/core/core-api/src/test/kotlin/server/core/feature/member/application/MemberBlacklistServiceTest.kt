package server.core.feature.member.application

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.core.feature.member.infra.BlackListSet
import test.UnitTest

class MemberBlacklistServiceTest : UnitTest() {
    @Test
    fun `탈퇴 이벤트를 받으면 블랙리스트에 등록한다`() {
        val blackListSet = mockk<BlackListSet>()
        every { blackListSet.add(7L) } returns true
        val service = MemberBlacklistService(blackListSet)

        service.addToBlacklist(MemberUnjoinEvent(memberId = 7L))

        verify(exactly = 1) { blackListSet.add(7L) }
    }

    @Test
    fun `블랙리스트 등록 실패 예외는 전파한다`() {
        val blackListSet = mockk<BlackListSet>()
        every { blackListSet.add(7L) } throws IllegalStateException("redis down")
        val service = MemberBlacklistService(blackListSet)

        shouldThrow<IllegalStateException> {
            service.addToBlacklist(MemberUnjoinEvent(memberId = 7L))
        }
    }
}
