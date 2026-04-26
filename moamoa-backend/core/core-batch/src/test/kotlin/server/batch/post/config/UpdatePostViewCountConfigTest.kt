package server.batch.post.config

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.ItemStreamReader
import org.springframework.transaction.PlatformTransactionManager
import server.batch.post.dto.PostViewCount
import server.batch.post.reader.UpdatePostViewCountReader
import server.batch.post.writer.UpdatePostViewCountWriter
import test.UnitTest

class UpdatePostViewCountConfigTest : UnitTest() {

    @Test
    fun `reader writer로 step을 구성한다`() {
        val reader = mockk<UpdatePostViewCountReader>()
        val writer = mockk<UpdatePostViewCountWriter>(relaxed = true)
        val config = UpdatePostViewCountConfig(reader, writer)
        val jobRepository = mockk<JobRepository>(relaxed = true)
        val txManager = mockk<PlatformTransactionManager>(relaxed = true)
        val builtReader = mockk<ItemStreamReader<PostViewCount>>(relaxed = true)

        every { reader.build() } returns builtReader

        val step = config.updatePostViewCountStep(jobRepository, txManager)
        val job = config.updatePostViewCountJob(jobRepository, step)

        step.name shouldBe "updatePostViewCountStep"
        job.name shouldBe "updatePostViewCountJob"
    }
}
