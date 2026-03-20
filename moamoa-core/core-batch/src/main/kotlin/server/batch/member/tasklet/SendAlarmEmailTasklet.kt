package server.batch.member.tasklet

import kotlinx.coroutines.runBlocking
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import server.batch.member.dto.AlarmContent
import server.mail.MailContent
import server.mail.MailSender
import server.queue.QueueMemory
import server.queue.drain
import server.template.mail.MailTemplate
import server.template.mail.toTemplateArgs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger

@StepScope
@Component
internal class SendAlarmEmailTasklet(
    private val queueMemory: QueueMemory,
    private val mailSender: MailSender,
) : Tasklet {

    private val log = kLogger {}

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZONE_KST)
            .toLocalDate()
            .format(DATE_FORMATTER)

        var processed = 0
        var success = 0
        var failed = 0

        runBlocking {
            while (true) {
                val alarms = queueMemory.drain<AlarmContent>(ALARM_CONTENTS_KEY, max = 200)
                if (alarms.isEmpty()) break

                processed += alarms.size
                alarms.forEach { alarm ->
                    runCatching {
                        mailSender.send(alarm.toMailContent(date))
                    }.onSuccess {
                        success += 1
                    }.onFailure { ex ->
                        failed += 1
                        log.warn(ex) {
                            "[WORKER] Failed to send alarm email. memberId=${alarm.memberId}, email=${alarm.email}"
                        }
                    }
                }
            }
        }

        log.info {
            "[WORKER] sendAlarmEmailJob completed. processed=$processed, success=$success, failed=$failed"
        }
        return RepeatStatus.FINISHED
    }

    private fun AlarmContent.toMailContent(date: String): MailContent.Template {
        val template = MailTemplate.NewPosts(
            date = date,
            count = techBlog.sumOf { it.posts.size }.toLong(),
            postSummaries = techBlog.flatMap { techBlog ->
                techBlog.posts.map { post ->
                    MailTemplate.NewPosts.PostSummary(
                        techBlogId = techBlog.techBlogId,
                        techBlogIcon = techBlog.techBlogIcon,
                        techBlogName = techBlog.techBlogTitle,
                        title = post.postTitle,
                        url = toPostUrl(post.postId),
                    )
                }
            },
            techBlogPosts = techBlog.map { techBlog ->
                MailTemplate.NewPosts.TechBlogPosts(
                    techBlogIcon = techBlog.techBlogIcon,
                    techBlogName = techBlog.techBlogTitle,
                    posts = techBlog.posts.map { post ->
                        MailTemplate.NewPosts.TechBlogPosts.PostDetail(
                            title = post.postTitle,
                            description = post.postDescription,
                            thumbnail = post.postThumbnail,
                            url = toPostUrl(post.postId),
                        )
                    }
                )
            }
        )

        return MailContent.Template(
            to = email,
            subject = SUBJECT,
            path = template.path,
            args = template.toTemplateArgs()
        )
    }

    private fun toPostUrl(postId: Long) = "https://moamoa.dev/post/${postId}"

    companion object {
        private const val ALARM_CONTENTS_KEY = "ALARM_CONTENTS"
        private const val SUBJECT = "[모아모아] 구독하신 블로그의 새 글이 도착했습니다!"
        private val ZONE_KST = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd")
    }
}
