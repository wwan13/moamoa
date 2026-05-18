package server.core.feature.member.infra

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import server.set.SetMemory
import test.UnitTest

class BlackListSetTest : UnitTest() {
    @Test
    fun `contains 는 member id 문자열로 membership 조회를 위임한다`() {
        val setMemory = mockk<SetMemory>()
        every { setMemory.contains("MEMBER:BLACKLIST", "21") } returns true
        val blackListSet = BlackListSet(setMemory)

        blackListSet.contains(21L)

        verify(exactly = 1) { setMemory.contains("MEMBER:BLACKLIST", "21") }
    }

    @Test
    fun `add 는 member id 문자열로 set 추가를 위임한다`() {
        val setMemory = mockk<SetMemory>()
        every { setMemory.add("MEMBER:BLACKLIST", "21") } returns true
        val blackListSet = BlackListSet(setMemory)

        blackListSet.add(21L)

        verify(exactly = 1) { setMemory.add("MEMBER:BLACKLIST", "21") }
    }
}
