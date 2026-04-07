package server.batch.member.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import server.batch.common.transaction.AfterCommitExecutor
import server.batch.member.dto.AlarmContent
import server.queue.QueueMemory

@Component
internal class GenerateAlarmContentWriter(
    private val queueMemory: QueueMemory,
    private val afterCommitExecutor: AfterCommitExecutor
) : ItemWriter<AlarmContent> {

    override fun write(chunk: Chunk<out AlarmContent?>) {
        val items = chunk.filterNotNull()
        if (items.isEmpty()) {
            return
        }

        afterCommitExecutor.execute {
            queueMemory.delete("ALARM_CONTENTS")
            queueMemory.rPushAll("ALARM_CONTENTS", items)
        }
    }
}