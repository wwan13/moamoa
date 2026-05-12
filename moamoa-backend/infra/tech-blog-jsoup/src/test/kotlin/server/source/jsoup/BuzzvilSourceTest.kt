package server.source.jsoup

import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BuzzvilSourceTest {

    private val source = BuzzvilSource()

    @Test
    fun `현재 Buzzvil Next payload의 posts 배열을 파싱한다`() {
        val doc = Jsoup.parse(
            """
            <html>
              <body>
                <script>
                  self.__next_f.push([1,"{\"total\":91,\"basePath\":\"/blog\"}],[\"${'$'}\",\"${'$'}L16\",null,{\"items\":[{\"slug\":\"feature-flag-api-p99-latency-improvement\",\"title\":\"Feature Flag API의 p99 레이턴시 개선기 (+오픈소스 기여)\",\"summary\":\"go-feature-flag의 동기 flush가 mutex를 잡고 S3 업로드/Jitter sleep을 수행하면서 발생한 p99 spike를 추적하고, AsyncExporter 도입과 OSS PR로 해결한 과정.\",\"date\":\"Sat May 02 2026 00:00:00 GMT+0000 (Coordinated Universal Time)\",\"author\":\"Elric Lim\",\"tags\":[\"feature-flag\",\"go\",\"opensource\"],\"coverUrl\":\"/blog/feature-flag-api-p99-latency-improvement/thumbnail.png\",\"category\":\"Backend\"},{\"slug\":\"nobody-owned-the-website-now-everybody-does\",\"title\":\"Nobody Owned the Website. Now Everybody Does.\",\"summary\":\"솔직한 답은 \\\"마케팅팀이 일단은요\\\" 사이 어딘가일 겁니다.\",\"date\":\"Wed, 15 Apr 2026 01:00:00 +0000\",\"author\":\"Maxence Mauduit\",\"tags\":[\"design-system\",\"ai\"],\"coverUrl\":\"/blog/nobody-owned-the-website-now-everybody-does/cover.png\",\"category\":\"Design\"},{\"slug\":\"content-recommendation\",\"title\":\"콘텐츠 추천 개선기\",\"summary\":\"ISO offset 날짜 형식 글입니다.\",\"date\":\"2019-07-09T00:00:00+09:00\",\"author\":\"Buzzvil\",\"tags\":[\"Data\"],\"coverUrl\":\"/blog/content-recommendation/cover.png\",\"category\":\"Data & ML\"},{\"slug\":\"bad-date\",\"title\":\"잘못된 날짜\",\"summary\":\"날짜가 잘못된 글입니다.\",\"date\":\"not-a-date\",\"author\":\"Buzzvil\",\"tags\":[],\"coverUrl\":\"/blog/bad-date/cover.png\",\"category\":\"Backend\"}]}"]);
                </script>
              </body>
            </html>
            """.trimIndent(),
            "https://tech.buzzvil.com/blog"
        )

        val posts = source.parsePosts(doc)

        posts.size shouldBe 3
        posts[0].key shouldBe "feature-flag-api-p99-latency-improvement"
        posts[0].title shouldBe "Feature Flag API의 p99 레이턴시 개선기 (+오픈소스 기여)"
        posts[0].description shouldBe "go-feature-flag의 동기 flush가 mutex를 잡고 S3 업로드/Jitter sleep을 수행하면서 발생한 p99 spike를 추적하고, AsyncExporter 도입과 OSS PR로 해결한 과정."
        posts[0].tags shouldBe listOf("Backend", "feature-flag", "go", "opensource")
        posts[0].thumbnail shouldBe "https://tech.buzzvil.com/blog/feature-flag-api-p99-latency-improvement/thumbnail.png"
        posts[0].publishedAt shouldBe LocalDateTime.of(2026, 5, 2, 0, 0)
        posts[0].url shouldBe "https://tech.buzzvil.com/blog/feature-flag-api-p99-latency-improvement"
        posts[1].description shouldBe "솔직한 답은 \"마케팅팀이 일단은요\" 사이 어딘가일 겁니다."
        posts[1].publishedAt shouldBe LocalDateTime.of(2026, 4, 15, 1, 0)
        posts[2].key shouldBe "content-recommendation"
        posts[2].publishedAt shouldBe LocalDateTime.of(2019, 7, 9, 0, 0)
    }
}
