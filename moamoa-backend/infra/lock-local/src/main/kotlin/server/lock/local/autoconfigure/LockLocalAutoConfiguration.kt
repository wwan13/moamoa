package server.lock.local.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.lock.ReentrantKeyedLock

@AutoConfiguration
@ConditionalOnMissingBean(name = ["coroutineMutexLock"])
@Import(ReentrantKeyedLock::class)
class LockLocalAutoConfiguration
