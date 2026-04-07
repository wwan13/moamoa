package server.batch.event.config

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
import server.batch.event.tasklet.DeleteEventOutboxTasklet

@Configuration
internal class DeleteEventOutboxJobConfig {

    @Bean
    fun deleteEventOutboxJob(
        jobRepository: JobRepository,
        deleteEventOutboxStep: Step
    ): Job = JobBuilder("deleteEventOutboxJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(deleteEventOutboxStep)
        .build()

    @Bean
    fun deleteEventOutboxStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager,
        tasklet: DeleteEventOutboxTasklet
    ): TaskletStep = StepBuilder("deleteEventOutboxStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(tasklet, txManager)
        .build()
}
