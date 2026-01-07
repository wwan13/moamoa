package server.post.config

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
import server.post.tasklet.SyncPostBookmarkCountTasklet

@Configuration
class SyncPostBookmarkCountConfig {

    @Bean
    fun syncPostBookmarkCountJob(
        jobRepository: JobRepository,
        syncPostBookmarkCountStep: Step
    ): Job = JobBuilder("syncPostBookmarkCountJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(syncPostBookmarkCountStep)
        .build()

    @Bean
    fun syncPostBookmarkCountStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: SyncPostBookmarkCountTasklet
    ): TaskletStep = StepBuilder("syncPostBookmarkCountStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}
