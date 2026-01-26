package server.batch.post.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.batch.post.dto.PostCategory
import server.batch.post.dto.PostSummary
import server.batch.post.processor.CategorizingPostProcessor
import server.batch.post.reader.CategorizingPostReader
import server.batch.post.writer.CategorizingPostWriter

@Configuration
internal class CategorizingPostConfig(
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