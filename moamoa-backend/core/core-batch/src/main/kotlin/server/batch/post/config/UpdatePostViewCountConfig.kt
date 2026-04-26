package server.batch.post.config

import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.batch.post.dto.PostViewCount
import server.batch.post.reader.UpdatePostViewCountReader
import server.batch.post.writer.UpdatePostViewCountWriter

@Configuration
internal class UpdatePostViewCountConfig(
    private val reader: UpdatePostViewCountReader,
    private val writer: UpdatePostViewCountWriter
) {
    @Bean
    fun updatePostViewCountJob(
        jobRepository: JobRepository,
        updatePostViewCountStep: Step,
    ) = JobBuilder("updatePostViewCountJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(updatePostViewCountStep)
        .build()

    @Bean
    fun updatePostViewCountStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
    ) = StepBuilder("updatePostViewCountStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<PostViewCount, PostViewCount>(100, txManager)
        .reader(reader.build())
        .writer(writer)
        .build()
}
