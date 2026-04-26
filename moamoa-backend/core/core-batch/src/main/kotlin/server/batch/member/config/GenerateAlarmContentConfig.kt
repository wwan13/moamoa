package server.batch.member.config

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import server.batch.member.dto.AlarmContent
import server.batch.member.dto.MemberData
import server.batch.member.processor.GenerateAlarmContentProcessor
import server.batch.member.reader.GenerateAlarmContentReader
import server.batch.member.writer.GenerateAlarmContentWriter

@Configuration
internal class GenerateAlarmContentConfig(
    private val reader: GenerateAlarmContentReader,
    private val processor: GenerateAlarmContentProcessor,
    private val writer: GenerateAlarmContentWriter
) {

    @Bean
    fun generateAlarmContentJob(
        jobRepository: JobRepository,
        generateAlarmContentStep: Step,
    ): Job = JobBuilder("generateAlarmContentJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .start(generateAlarmContentStep)
        .build()

    @Bean
    fun generateAlarmContentStep(
        jobRepository: JobRepository,
        txManager: PlatformTransactionManager
    ) = StepBuilder("generateAlarmContentStep", jobRepository)
        .allowStartIfComplete(true)
        .chunk<MemberData, AlarmContent>(100, txManager)
        .reader(reader.build())
        .processor(processor)
        .writer(writer)
        .build()
}