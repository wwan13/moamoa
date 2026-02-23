package server.resilient

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import server.shared.lock.KeyedLock

@Component("resilientLock")
@Primary
internal class ResilientLock(
    @param:Qualifier("redissonLock")
    private val delegate: KeyedLock,
) : KeyedLock by delegate
