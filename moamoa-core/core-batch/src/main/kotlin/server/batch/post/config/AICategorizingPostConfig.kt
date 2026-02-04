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
import server.batch.post.processor.AICategorizingPostProcessor
import server.batch.post.reader.AICategorizingPostReader
import server.batch.post.writer.AICategorizingPostWriter

@Configuration
internal class AICategorizingPostConfig(
    private val reader: AICategorizingPostReader,
    private val processor: AICategorizingPostProcessor,
    private val writer: AICategorizingPostWriter,
) {
    @Bean
    fun aiCategorizingPostJob(
        jobRepository: JobRepository,
        aiCategorizingPostStep: Step,
    ): Job = JobBuilder("aiCategorizingPostJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(aiCategorizingPostStep)
        .build()

    @Bean
    fun aiCategorizingPostStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ): Step = StepBuilder("aiCategorizingPostStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<List<PostSummary>, List<PostCategory>>(20, txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build()
}
