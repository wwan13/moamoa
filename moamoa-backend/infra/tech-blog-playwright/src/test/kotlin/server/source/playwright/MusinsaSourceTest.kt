package server.source.playwright

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import test.UnitTest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MusinsaSourceTest : UnitTest() {

    private val sut = MusinsaSource()
    private val fixedSut = MusinsaSource(
        clock = Clock.fixed(
            Instant.parse("2026-05-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
        )
    )

    @Test
    fun `브라우저에서 추출한 게시글 정보를 TechBlogPost 로 변환한다`() {
        val post = sut.parsePostSnapshot(
            MusinsaSource.MusinsaPostSnapshot(
                url = "https://techblog.musinsa.com/sample-title-a1976d599e83?source=home",
                title = "무신사 메가스토어 성수: 보이지 않는 기술, 선명해지는 경험",
                description = "기술을 지워야 보이는 것들",
                thumbnail = "https://miro.medium.com/sample.png",
                publishedAt = "2026-05-04T01:01:01.000Z",
                tags = listOf("retail-technology", "musinsa", "musinsa"),
            )
        )

        post.key shouldBe "a1976d599e83"
        post.title shouldBe "무신사 메가스토어 성수: 보이지 않는 기술, 선명해지는 경험"
        post.description shouldBe "기술을 지워야 보이는 것들"
        post.tags shouldBe listOf("retail-technology", "musinsa")
        post.thumbnail shouldBe "https://miro.medium.com/sample.png"
        post.publishedAt shouldBe LocalDateTime.of(2026, 5, 4, 1, 1, 1)
        post.url shouldBe "https://techblog.musinsa.com/sample-title-a1976d599e83"
    }

    @Test
    fun `발행일을 찾지 못하면 게시글 파싱에 실패한다`() {
        assertThrows<IllegalArgumentException> {
            sut.parsePostSnapshot(
                MusinsaSource.MusinsaPostSnapshot(
                    url = "https://techblog.musinsa.com/sample-title-a1976d599e83",
                    title = "제목",
                    description = "",
                    thumbnail = "",
                    publishedAt = "",
                    tags = emptyList(),
                )
            )
        }
    }

    @Test
    fun `비정상 연도는 게시글 파싱에 실패한다`() {
        assertThrows<IllegalArgumentException> {
            sut.parsePostSnapshot(
                MusinsaSource.MusinsaPostSnapshot(
                    url = "https://techblog.musinsa.com/sample-title-a1976d599e83",
                    title = "제목",
                    description = "",
                    thumbnail = "",
                    publishedAt = "169087565-03-15 04:51:43",
                    tags = emptyList(),
                )
            )
        }
    }

    @Test
    fun `상대 발행일을 파싱한다`() {
        val post = fixedSut.parsePostSnapshot(
            MusinsaSource.MusinsaPostSnapshot(
                url = "https://techblog.musinsa.com/sample-title-a1976d599e83",
                title = "제목",
                description = "",
                thumbnail = "",
                publishedAt = "Added 2d ago",
                tags = emptyList(),
            )
        )

        post.publishedAt shouldBe LocalDateTime.of(2026, 5, 6, 12, 0, 0)
    }

    @Test
    fun `연도가 없는 월일 발행일은 현재 연도로 파싱한다`() {
        val post = fixedSut.parsePostSnapshot(
            MusinsaSource.MusinsaPostSnapshot(
                url = "https://techblog.musinsa.com/sample-title-a1976d599e83",
                title = "제목",
                description = "",
                thumbnail = "",
                publishedAt = "Added Apr 6",
                tags = emptyList(),
            )
        )

        post.publishedAt shouldBe LocalDateTime.of(2026, 4, 6, 0, 0, 0)
    }
}
