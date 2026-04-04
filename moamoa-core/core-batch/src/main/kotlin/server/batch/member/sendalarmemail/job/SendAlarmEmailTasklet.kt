package server.batch.member.sendalarmemail.job

import org.springframework.stereotype.Component
import server.batch.common.job.CoroutineBatchJob
import server.batch.member.generatealarmcontent.dto.AlarmContent
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

@Component
internal class SendAlarmEmailTasklet(
    private val queueMemory: QueueMemory,
    private val mailSender: MailSender,
) : CoroutineBatchJob {

    private val log = kLogger {}
    override val jobName: String = "sendAlarmEmailJob"

    override suspend fun run(parameters: Map<String, String>) {
        val date = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZONE_KST)
            .toLocalDate()
            .format(DATE_FORMATTER)

        var processed = 0
        var success = 0
        var failed = 0

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

        log.info {
            "[WORKER] sendAlarmEmailJob completed. processed=$processed, success=$success, failed=$failed"
        }
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
                    techBlogId = techBlog.techBlogId,
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
