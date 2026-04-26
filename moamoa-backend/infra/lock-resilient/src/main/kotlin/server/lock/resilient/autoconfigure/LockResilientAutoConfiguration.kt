package server.lock.resilient.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.config.ResilientLockProperties
import server.lock.ResilientKeyedLock
import server.lock.ResilientKeyedLockAspect
import server.lock.ResilientKeyedLockMethodInvoker
import server.lock.ResilientKeyedLockRouter

@AutoConfiguration
@ConditionalOnMissingBean(name = ["resilientKeyedLock"])
@Import(
    ResilientLockProperties::class,
    ResilientKeyedLockRouter::class,
    ResilientKeyedLockMethodInvoker::class,
    ResilientKeyedLock::class,
    ResilientKeyedLockAspect::class,
)
class LockResilientAutoConfiguration
