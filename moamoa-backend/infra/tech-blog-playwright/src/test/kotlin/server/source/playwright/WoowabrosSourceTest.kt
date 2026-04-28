package server.source.playwright

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest
import java.time.LocalDateTime

class WoowabrosSourceTest : UnitTest() {

    private val sut = WoowabrosSource()

    @Test
    fun `ajax 응답에서 게시글 목록과 최대 페이지를 파싱한다`() {
        val body =
            """
            {
              "success": true,
              "data": {
                "posts": [
                  {
                    "permalink": "https://techblog.woowahan.com/26177/",
                    "date": "Apr.17.2026",
                    "excerpt": "요약 1",
                    "post_title": "첫 글"
                  },
                  {
                    "permalink": "https://techblog.woowahan.com/26162/",
                    "date": "2026. 04. 03.",
                    "excerpt": "요약 2",
                    "post_title": "둘째 글"
                  }
                ],
                "pagination": {
                  "max": 52
                }
              }
            }
            """.trimIndent()

        val page = sut.parsePage(body)

        page.maxPage shouldBe 52
        page.posts shouldHaveSize 2
        page.posts[0].key shouldBe "26177"
        page.posts[0].publishedAt shouldBe LocalDateTime.of(2026, 4, 17, 0, 0)
        page.posts[1].title shouldBe "둘째 글"
    }

    @Test
    fun `ajax 응답에 중복 permalink 가 있으면 하나만 남긴다`() {
        val body =
            """
            {
              "success": true,
              "data": {
                "posts": [
                  {
                    "permalink": "https://techblog.woowahan.com/26177/",
                    "date": "Apr.17.2026",
                    "excerpt": "요약 1",
                    "post_title": "첫 글"
                  },
                  {
                    "permalink": "https://techblog.woowahan.com/26177/",
                    "date": "Apr.17.2026",
                    "excerpt": "요약 1-중복",
                    "post_title": "첫 글 중복"
                  }
                ],
                "pagination": {
                  "max": 52
                }
              }
            }
            """.trimIndent()

        val page = sut.parsePage(body)

        page.posts shouldHaveSize 1
        page.posts[0].title shouldBe "첫 글"
    }
}
