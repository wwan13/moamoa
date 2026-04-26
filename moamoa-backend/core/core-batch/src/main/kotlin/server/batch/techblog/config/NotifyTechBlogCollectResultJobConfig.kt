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
import server.batch.techblog.tasklet.NotifyTechBlogCollectResultTasklet

@Configuration
internal class NotifyTechBlogCollectResultJobConfig {

    @Bean
    fun notifyTechBlogCollectResultJob(
        jobRepository: JobRepository,
        notifyTechBlogCollectResultStep: Step,
    ): Job = JobBuilder("notifyTechBlogCollectResultJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(notifyTechBlogCollectResultStep)
        .build()

    @Bean
    fun notifyTechBlogCollectResultStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: NotifyTechBlogCollectResultTasklet,
    ): TaskletStep = StepBuilder("notifyTechBlogCollectResultStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}
