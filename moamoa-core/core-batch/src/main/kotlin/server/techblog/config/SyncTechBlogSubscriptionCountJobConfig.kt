package server.techblog.config

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
import server.techblog.tasklet.SyncTechBlogSubscriptionCountTasklet

@Configuration
class SyncTechBlogSubscriptionCountJobConfig {

    @Bean
    fun syncTechBlogSubscriptionCountJob(
        jobRepository: JobRepository,
        syncTechBlogSubscriptionCountStep: Step
    ): Job = JobBuilder("syncTechBlogSubscriptionCountJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(syncTechBlogSubscriptionCountStep)
        .build()

    @Bean
    fun syncTechBlogSubscriptionCountStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: SyncTechBlogSubscriptionCountTasklet
    ): TaskletStep = StepBuilder("syncTechBlogSubscriptionCountStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}