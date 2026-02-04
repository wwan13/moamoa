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
import server.batch.post.dto.PostSummary
import server.batch.post.dto.PreCategorizingPostResult
import server.batch.post.processor.PreCategorizingPostProcessor
import server.batch.post.reader.PreCategorizingPostReader
import server.batch.post.writer.PreCategorizingPostWriter

@Configuration
internal class PreCategorizingPostConfig(
    private val reader: PreCategorizingPostReader,
    private val processor: PreCategorizingPostProcessor,
    private val writer: PreCategorizingPostWriter,
) {
    @Bean
    fun preCategorizingPostJob(
        jobRepository: JobRepository,
        preCategorizingPostStep: Step,
    ): Job = JobBuilder("preCategorizingPostJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(preCategorizingPostStep)
        .build()

    @Bean
    fun preCategorizingPostStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ): Step = StepBuilder("preCategorizingPostStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<List<PostSummary>, PreCategorizingPostResult>(100, txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build()
}
