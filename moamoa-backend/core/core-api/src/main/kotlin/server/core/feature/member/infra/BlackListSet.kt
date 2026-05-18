package server.core.feature.member.infra

import org.springframework.stereotype.Component
import server.set.SetMemory

@Component
class BlackListSet(
    private val setMemory: SetMemory,
) {
    fun add(memberId: Long): Boolean = setMemory.add(KEY, memberId.toString())

    fun contains(memberId: Long): Boolean = setMemory.contains(KEY, memberId.toString())

    companion object {
        private const val KEY = "MEMBER:BLACKLIST"
    }
}
