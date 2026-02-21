package server.messaging.health

import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.stereotype.Component

@Component
internal class RedisRecoveryActionRunner(
    private val beanFactory: ListableBeanFactory,
) {
    suspend fun runAll() {
        beanFactory.getBeansOfType(RedisRecoveryAction::class.java)
            .values
            .forEach { it.onRecovered() }
    }
}
