package server.batch.techblog.config

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
import server.batch.techblog.tasklet.SyncSubscriptionCountTasklet

@Configuration
internal class SyncSubscriptionCountJobConfig {

    @Bean
    fun syncSubscriptionCountJob(
        jobRepository: JobRepository,
        syncSubscriptionCountStep: Step
    ): Job = JobBuilder("syncSubscriptionCountJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(syncSubscriptionCountStep)
        .build()

    @Bean
    fun syncSubscriptionCountStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: SyncSubscriptionCountTasklet
    ): TaskletStep = StepBuilder("syncSubscriptionCountStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}