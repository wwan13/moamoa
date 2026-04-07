package server.batch.techblog.config

import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.support.transaction.ResourcelessTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.batch.techblog.dto.PostData
import server.batch.techblog.dto.TechBlogKey
import server.batch.techblog.processor.FetchTechBlogPostProcessor
import server.batch.techblog.reader.FetchTechBlogPostReader
import server.batch.techblog.reader.PersistTechBlogPostReader
import server.batch.techblog.writer.FetchTechBlogPostWriter
import server.batch.techblog.writer.PersistTechBlogPostWriter

@Configuration
internal class CollectTechBlogPostConfig(
    private val reader: FetchTechBlogPostReader,
    private val persistReader: PersistTechBlogPostReader,
    private val processor: FetchTechBlogPostProcessor,
    private val fetchWriter: FetchTechBlogPostWriter,
    private val persistWriter: PersistTechBlogPostWriter
) {
    @Bean
    fun collectTechBlogPostJob(
        jobRepository: JobRepository,
        fetchTechBlogPostStep: Step,
        persistTechBlogPostStep: Step,
    ) = JobBuilder("collectTechBlogPostJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(fetchTechBlogPostStep)
        .next(persistTechBlogPostStep)
        .build()

    @Bean
    fun fetchTechBlogPostStep(
        jobRepository: JobRepository,
    ) = StepBuilder("fetchTechBlogPostStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<TechBlogKey, List<PostData>>(20, ResourcelessTransactionManager())
        .reader(reader.build())
        .processor(processor)
        .writer(fetchWriter)
        .build()

    @Bean
    fun persistTechBlogPostStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ) = StepBuilder("persistTechBlogPostStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<List<PostData>, List<PostData>>(20, txManager)
        .reader(persistReader)
        .writer(persistWriter)
        .build()
}
