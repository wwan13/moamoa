package server.techblog.config

import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.techblog.dto.PostData
import server.techblog.dto.TechBlogKey
import server.techblog.processor.FetchTechBlogPostsProcessor
import server.techblog.reader.FetchTechBlogPostsReader
import server.techblog.writer.FetchTechBlogPostsWriter

@Configuration
class FetchTechBlogPostsConfig(
    private val reader: FetchTechBlogPostsReader,
    private val processor: FetchTechBlogPostsProcessor,
    private val writer: FetchTechBlogPostsWriter
) {
    @Bean
    fun fetchTechBlogPostsJob(
        jobRepository: JobRepository,
        fetchTechBlogPostsStep: Step,
    ) = JobBuilder("fetchTechBlogPostsJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(fetchTechBlogPostsStep)
        .build()

    @Bean
    fun fetchTechBlogPostsStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ) = StepBuilder("fetchTechBlogPostsStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<TechBlogKey, List<PostData>>(100, txManager)
        .reader(reader.build())
        .processor(processor)
        .writer(writer)
        .build()
}