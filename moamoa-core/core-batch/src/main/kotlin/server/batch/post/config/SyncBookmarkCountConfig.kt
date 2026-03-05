package server.batch.post.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.TaskletStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.batch.post.tasklet.SyncBookmarkCountTasklet

@Configuration
internal class SyncBookmarkCountConfig {

    @Bean
    fun syncBookmarkCountJob(
        jobRepository: JobRepository,
        syncBookmarkCountStep: Step
    ): Job = JobBuilder("syncBookmarkCountJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(syncBookmarkCountStep)
        .build()

    @Bean
    fun syncBookmarkCountStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: SyncBookmarkCountTasklet
    ): TaskletStep = StepBuilder("syncBookmarkCountStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}
