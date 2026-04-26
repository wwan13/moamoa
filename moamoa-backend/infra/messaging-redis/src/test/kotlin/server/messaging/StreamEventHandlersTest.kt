package server.messaging

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import server.messaging.annotation.EventHandler
import server.messaging.annotation.TransactionEventHandler
import server.messaging.definition.EventStream
import test.UnitTest
import java.util.function.Supplier

class StreamEventHandlersTest : UnitTest() {
    @Test
    fun `이벤트 핸들러는 메서드 파라미터 타입으로 등록된다`() {
        val context = GenericApplicationContext().apply {
            registerBean("sampleHandler", SampleHandler::class.java, Supplier { SampleHandler() })
            refresh()
        }

        val handlers = StreamEventHandlers(
            context = context,
            beanFactory = context.beanFactory,
            txManager = null,
        )

        val handler = handlers.find(EventStream.MONITORING, SampleEvent::class.simpleName!!)

        handler?.payloadClass shouldBe SampleEvent::class.java
        handlers.streams() shouldContain EventStream.MONITORING
        context.close()
    }

    @Test
    fun `트랜잭션 이벤트 핸들러도 메서드 파라미터 타입으로 등록되고 실행된다`() {
        val context = GenericApplicationContext().apply {
            registerBean("transactionalHandler", TransactionalHandler::class.java, Supplier { TransactionalHandler() })
            refresh()
        }

        val handlers = StreamEventHandlers(
            context = context,
            beanFactory = context.beanFactory,
            txManager = TestTransactionManager(),
        )

        val handler = requireNotNull(
            handlers.find(EventStream.COUNT_PROCESSING, TransactionSampleEvent::class.simpleName!!)
        )
        val bean = context.getBean("transactionalHandler", TransactionalHandler::class.java)
        val event = TransactionSampleEvent("payload")

        handler.payloadClass shouldBe TransactionSampleEvent::class.java

        @Suppress("UNCHECKED_CAST")
        (handler.handler as (TransactionSampleEvent) -> Unit).invoke(event)

        bean.handledEvent shouldBe event
        context.close()
    }

    private data class SampleEvent(
        val value: String,
    )

    private data class TransactionSampleEvent(
        val value: String,
    )

    private class SampleHandler {
        @EventHandler(stream = EventStream.MONITORING)
        fun handle(event: SampleEvent) {
            event.value.length
        }
    }

    private class TransactionalHandler {
        var handledEvent: TransactionSampleEvent? = null

        @TransactionEventHandler(EventStream.COUNT_PROCESSING)
        fun handle(event: TransactionSampleEvent) {
            handledEvent = event
        }
    }

    private class TestTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }
}
