package server.source.jsoup

import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KurlySourceTest {

    private val source = KurlySource()

    @Test
    fun `현재 컬리 블로그 카드 구조를 파싱한다`() {
        val doc = Jsoup.parse(
            """
            <main>
              <article class="group cursor-pointer">
                <a href="/blog/image-based-misdelivery-detection">
                  <div>
                    <img src="/_astro/cover.BKJdQYfQ_rxo1B.webp" alt="thumbnail">
                  </div>
                  <div>
                    <h2>현관문에도 얼굴이 있다: 배송 완료 사진 기반 On-device 오배송 탐지 시스템</h2>
                    <p>배송 완료 사진의 임베딩 유사도를 기반으로 오배송을 탐지하는 방법과, On-device 추론을 통해 서비스에 적용한 과정을 소개하는 글입니다.</p>
                    <div>
                      <span>AI/데이터</span>
                      <span>윤준호</span>
                      <span>·</span>
                      <time datetime="2026-04-27T15:00:00.000Z">2026년 04월 28일</time>
                    </div>
                  </div>
                </a>
              </article>
              <article class="group cursor-pointer">
                <a href="/blog/vibe-coding-with-claude-code">
                  <div>
                    <h2>Claude Code를 활용한 예측 가능한 바이브 코딩 전략</h2>
                    <p>에이전트 주도 개발에서 발생하는 문제를 해결하고, 컨텍스트를 정밀하게 관리하는 방법</p>
                    <div>
                      <span>AI/데이터</span>
                      <span>박재영</span>
                      <span>·</span>
                      <time datetime="2025-12-16T15:00:00.000Z">2025년 12월 17일</time>
                    </div>
                  </div>
                </a>
              </article>
            </main>
            """.trimIndent(),
            "https://helloworld.kurly.com/"
        )

        val posts = source.parsePosts(doc)

        posts.size shouldBe 2
        posts[0].key shouldBe "image-based-misdelivery-detection"
        posts[0].title shouldBe "현관문에도 얼굴이 있다: 배송 완료 사진 기반 On-device 오배송 탐지 시스템"
        posts[0].description shouldBe "배송 완료 사진의 임베딩 유사도를 기반으로 오배송을 탐지하는 방법과, On-device 추론을 통해 서비스에 적용한 과정을 소개하는 글입니다."
        posts[0].tags shouldBe listOf("AI/데이터")
        posts[0].thumbnail shouldBe "https://helloworld.kurly.com/_astro/cover.BKJdQYfQ_rxo1B.webp"
        posts[0].publishedAt shouldBe LocalDate.of(2026, 4, 28).atStartOfDay()
        posts[0].url shouldBe "https://helloworld.kurly.com/blog/image-based-misdelivery-detection"
        posts[1].thumbnail shouldBe ""
        posts[1].publishedAt shouldBe LocalDate.of(2025, 12, 17).atStartOfDay()
    }

    @Test
    fun `기존 컬리 블로그 카드 구조도 유지한다`() {
        val doc = Jsoup.parse(
            """
            <ul class="post-list">
              <li class="post-card">
                <a class="post-link" href="/blog/legacy-post">
                  <h3 class="post-title">Legacy Post</h3>
                  <p class="title-summary">Legacy description</p>
                  <span class="post-date">2025.12.24.</span>
                </a>
              </li>
            </ul>
            """.trimIndent(),
            "https://helloworld.kurly.com/"
        )

        val posts = source.parsePosts(doc)

        posts.size shouldBe 1
        posts[0].key shouldBe "legacy-post"
        posts[0].title shouldBe "Legacy Post"
        posts[0].description shouldBe "Legacy description"
        posts[0].publishedAt shouldBe LocalDate.of(2025, 12, 24).atStartOfDay()
        posts[0].url shouldBe "https://helloworld.kurly.com/blog/legacy-post"
    }
}
