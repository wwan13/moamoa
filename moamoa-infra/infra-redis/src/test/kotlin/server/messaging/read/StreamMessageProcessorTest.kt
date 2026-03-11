package server.messaging.read

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import server.messaging.MessageHandlerInvoker
import server.messaging.StreamEventHandlers
import server.messaging.definition.EventStream

class StreamMessageProcessorTest {

    @Test
    fun `핸들러가 없어도 WARN 로그를 남기지 않고 ack 한다`() {
        val appLogger = LoggerFactory.getLogger(StreamMessageProcessor::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        appLogger.addAppender(appender)

        val handlers = emptyHandlers()
        val processor = StreamMessageProcessor(
            handlers = handlers,
            objectMapper = jacksonObjectMapper(),
            messageHandlerInvoker = mockk<MessageHandlerInvoker>(relaxed = true),
        )
        val ops = mockk<StreamOperations<String, String, String>>(relaxed = true)
        val record = mockk<MapRecord<String, String, String>>()
        val recordId = RecordId.of("1-0")
        every { record.id } returns recordId
        every { record.value } returns mapOf(
            "type" to "BookmarkUpdatedEvent",
            "payload" to """{"postId":1}""",
            "eventId" to "e-1",
        )

        try {
            processor.handleRecords(
                stream = EventStream.MONITORING,
                ops = ops,
                records = listOf(record),
                launchScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            )
        } finally {
            appLogger.detachAppender(appender)
        }

        verify(exactly = 1) {
            ops.acknowledge(EventStream.MONITORING.channel.key, EventStream.MONITORING.consumerGroup, recordId)
        }
        val warnLogs = appender.list.filter { it.level == Level.WARN }
        assertTrue(warnLogs.isEmpty())
    }

    private fun emptyHandlers(): StreamEventHandlers {
        val context = mockk<ApplicationContext>()
        every { context.getType(any<String>()) } returns null

        val beanFactory = mockk<ConfigurableListableBeanFactory>()
        every { beanFactory.beanDefinitionNames } returns emptyArray()
        every { beanFactory.isSingleton(any()) } returns true

        return StreamEventHandlers(
            context = context,
            beanFactory = beanFactory,
            txManager = null,
        )
    }
}
