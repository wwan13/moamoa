package server.batch.member.tasklet

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import server.batch.member.dto.AlarmContent
import server.mail.MailContent
import server.mail.MailSender
import server.queue.QueueMemory
import server.template.mail.MailTemplate
import test.UnitTest

class SendAlarmEmailTaskletTest : UnitTest() {
    @Test
    fun `MailTemplate NewPosts 생성자에서 길이 초과 텍스트를 말줄임 처리한다`() {
        val summary = MailTemplate.NewPosts.PostSummary(
            techBlogIcon = "https://example.com/icon.png",
            techBlogName = "1234567890",
            title = "t".repeat(40),
            url = "https://example.com/post-1",
        )
        val detail = MailTemplate.NewPosts.TechBlogPosts.PostDetail(
            title = "a".repeat(40),
            description = "d".repeat(260),
            thumbnail = "https://example.com/thumb.png",
            url = "https://example.com/post-1",
        )

        summary.title.length shouldBe 36
        summary.title.endsWith("...") shouldBe true

        detail.title.length shouldBe 34
        detail.title.endsWith("...") shouldBe true

        detail.description.length shouldBe 254
        detail.description.endsWith("...") shouldBe true
    }

    @Test
    fun `큐가 비어 있으면 메일을 발송하지 않는다`() {
        val queueMemory = mockk<QueueMemory>()
        val mailSender = mockk<MailSender>(relaxed = true)
        val sut = SendAlarmEmailTasklet(queueMemory, mailSender, 1_742_454_400_000L)

        coEvery { queueMemory.drain("ALARM_CONTENTS", AlarmContent::class.java, 200) } returns emptyList()

        val result = sut.execute(
            contribution = contribution(),
            chunkContext = chunkContext(),
        )

        result shouldBe RepeatStatus.FINISHED
        coVerify(exactly = 1) { queueMemory.drain("ALARM_CONTENTS", AlarmContent::class.java, 200) }
        coVerify(exactly = 0) { mailSender.send(any<MailContent.Template>()) }
    }

    @Test
    fun `알람 컨텐츠를 템플릿 메일로 매핑해 발송한다`() {
        val queueMemory = mockk<QueueMemory>()
        val mailSender = mockk<MailSender>()
        val runId = 1_773_932_400_000L // 2026-03-20 00:00:00 KST
        val sut = SendAlarmEmailTasklet(queueMemory, mailSender, runId)
        val alarm = alarmContent(memberId = 10L, email = "member@example.com")
        val sent = mutableListOf<MailContent.Template>()

        coEvery {
            queueMemory.drain("ALARM_CONTENTS", AlarmContent::class.java, 200)
        } returnsMany listOf(listOf(alarm), emptyList())
        coEvery { mailSender.send(capture(sent)) } returns Unit

        val result = sut.execute(
            contribution = contribution(),
            chunkContext = chunkContext(),
        )

        result shouldBe RepeatStatus.FINISHED
        coVerify(exactly = 2) { queueMemory.drain("ALARM_CONTENTS", AlarmContent::class.java, 200) }
        coVerify(exactly = 1) { mailSender.send(any<MailContent.Template>()) }

        val template = sent.single()
        template.to shouldBe "member@example.com"
        template.subject shouldBe "모아모아 새 게시글 알림"
        template.path shouldBe "new-posts"
        template.args["date"] shouldBe "26.03.20"
        template.args["count"] shouldBe 2L

        val postSummaries = template.args["postSummaries"] as List<*>
        postSummaries.size shouldBe 2
        val firstSummary = postSummaries.first() as? MailTemplate.NewPosts.PostSummary
        firstSummary.shouldNotBeNull()
        firstSummary.techBlogName shouldBe "가비아"
        firstSummary.title shouldBe "첫번째 글"
        firstSummary.url shouldBe "https://example.com/post-1"

        val techBlogPosts = template.args["techBlogPosts"] as List<*>
        techBlogPosts.size shouldBe 1
        val firstTechBlog = techBlogPosts.first() as? MailTemplate.NewPosts.TechBlogPosts
        firstTechBlog.shouldNotBeNull()
        firstTechBlog.techBlogName shouldBe "가비아"
    }

    @Test
    fun `개별 발송 실패가 발생해도 다음 메일 발송을 계속한다`() {
        val queueMemory = mockk<QueueMemory>()
        val mailSender = mockk<MailSender>()
        val sut = SendAlarmEmailTasklet(queueMemory, mailSender, 1_773_932_400_000L)
        val first = alarmContent(memberId = 11L, email = "first@example.com")
        val second = alarmContent(memberId = 12L, email = "second@example.com")

        coEvery {
            queueMemory.drain("ALARM_CONTENTS", AlarmContent::class.java, 200)
        } returnsMany listOf(listOf(first, second), emptyList())
        coEvery { mailSender.send(any<MailContent.Template>()) } throws IllegalStateException("mail down") andThen Unit

        val result = sut.execute(
            contribution = contribution(),
            chunkContext = chunkContext(),
        )

        result shouldBe RepeatStatus.FINISHED
        coVerify(exactly = 2) { mailSender.send(any<MailContent.Template>()) }
    }

    private fun alarmContent(memberId: Long, email: String): AlarmContent =
        AlarmContent(
            memberId = memberId,
            email = email,
            techBlog = listOf(
                AlarmContent.TechBlog(
                    techBlogId = 1L,
                    techBlogTitle = "가비아",
                    techBlogIcon = "https://example.com/icon.png",
                    posts = listOf(
                        AlarmContent.TechBlog.Post(
                            postId = 101L,
                            postTitle = "첫번째 글",
                            postDescription = "첫번째 설명",
                            postThumbnail = "https://example.com/thumb-1.png",
                            postUrl = "https://example.com/post-1",
                        ),
                        AlarmContent.TechBlog.Post(
                            postId = 102L,
                            postTitle = "두번째 글",
                            postDescription = "두번째 설명",
                            postThumbnail = "https://example.com/thumb-2.png",
                            postUrl = "https://example.com/post-2",
                        ),
                    )
                )
            )
        )

    private fun contribution(): StepContribution {
        val stepExecution = StepExecution("sendAlarmEmailStep", JobExecution(1L))
        return StepContribution(stepExecution)
    }

    private fun chunkContext(): ChunkContext {
        val stepExecution = StepExecution("sendAlarmEmailStep", JobExecution(1L))
        return ChunkContext(StepContext(stepExecution))
    }
}
