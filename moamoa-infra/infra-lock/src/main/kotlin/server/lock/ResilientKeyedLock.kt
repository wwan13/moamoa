package server.lock

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component("resilientKeyedLock")
@Primary
internal class ResilientKeyedLock(
    @param:Qualifier("redissonLock")
    private val delegate: KeyedLock,
) : KeyedLock by delegate
