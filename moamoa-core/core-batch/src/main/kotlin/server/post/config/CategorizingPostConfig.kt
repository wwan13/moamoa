package server.post.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.post.dto.PostCategory
import server.post.dto.PostSummary
import server.post.dto.PostViewCount
import server.post.processor.CategorizingPostProcessor
import server.post.processor.UpdatePostViewCountProcessor
import server.post.reader.CategorizingPostReader
import server.post.reader.UpdatePostViewCountReader
import server.post.writer.CategorizingPostWriter
import server.post.writer.UpdatePostViewCountWriter

@Configuration
class CategorizingPostConfig(
    private val reader: CategorizingPostReader,
    private val processor: CategorizingPostProcessor,
    private val writer: CategorizingPostWriter,
) {
    @Bean
    fun categorizingPostJob(
        jobRepository: JobRepository,
        categorizingPostStep: Step,
    ): Job = JobBuilder("categorizingPostJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(categorizingPostStep)
        .build()

    @Bean
    fun categorizingPostStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ): Step = StepBuilder("categorizingPostStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<List<PostSummary>, List<PostCategory>>(10, txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build()
}