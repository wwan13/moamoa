package server.batch.member.generatealarmcontent.writer

import org.springframework.stereotype.Component
import server.batch.member.generatealarmcontent.dto.AlarmContent
import server.queue.QueueMemory

@Component
internal class GenerateAlarmContentWriter(
    private val queueMemory: QueueMemory,
) {

    suspend fun write(items: List<AlarmContent>) {
        if (items.isEmpty()) {
            return
        }

        queueMemory.delete("ALARM_CONTENTS")
        queueMemory.rPushAll("ALARM_CONTENTS", items)
    }
}
