package server.core.feature.member.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.member.infra.BlackListSet
import server.messaging.annotation.EventHandler
import server.messaging.definition.EventStream

@Service
class MemberBlacklistService(
    private val blackListSet: BlackListSet,
) {
    @EventHandler(EventStream.DEFAULT)
    fun addToBlacklist(event: MemberUnjoinEvent) {
        blackListSet.add(event.memberId)
    }
}
