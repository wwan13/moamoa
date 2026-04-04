package server.batch.member.generatealarmcontent.job

import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.member.generatealarmcontent.processor.GenerateAlarmContentProcessor
import server.batch.member.generatealarmcontent.reader.GenerateAlarmContentReader
import server.batch.member.generatealarmcontent.writer.GenerateAlarmContentWriter

@Component
internal class GenerateAlarmContentCoroutineJob(
    private val reader: GenerateAlarmContentReader,
    private val processor: GenerateAlarmContentProcessor,
    private val writer: GenerateAlarmContentWriter,
) : CoroutineBatchJob {

    override val jobName: String = "generateAlarmContentJob"

    override suspend fun run(parameters: Map<String, String>) {
        val members = reader.readAll()
        if (members.isEmpty()) return

        val subscriptionsByMemberId = processor.loadSubscriptions()
        val postsByTechBlogId = processor.loadNewPosts()

        val alarms = members.mapNotNull { member ->
            processor.process(member, subscriptionsByMemberId, postsByTechBlogId)
        }
        writer.write(alarms)
    }
}
