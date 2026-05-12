package server.source.jsoup

import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KakaoMobilitySourceTest {

    private val source = KakaoMobilitySource()

    @Test
    fun `techblogs 데이터 번들에서 전체 글을 파싱한다`() {
        val doc = Jsoup.parse(
            """
            <html>
              <head>
                <link rel="modulepreload" href="/assets/chunks/techblogs.data.CzP5cYqH.js">
              </head>
            </html>
            """.trimIndent(),
            "https://developers.kakaomobility.com/techblogs/"
        )

        val posts = source.parseDataAsset(doc) {
            """
            const t=JSON.parse(`[{"title":"도로명주소와 건축물·토지 데이터 레이어링으로 도출하는 공간 인사이트","description":"도로명주소와 지번주소를 매개로 건축물대장과 토지특성도 데이터를 결합하여, 건물의 입체적 프로필과 땅의 물리적 환경을 분석하고 모빌리티 서비스의 비즈니스 가치를 창출하는 기술적 로직을 공유합니다.","date":"2026.04.08","author":"지미(박지민)","image":"https://t1.kakaocdn.net/km_developers/docs/techblogs/address-structure-3/thumbnail.png","category":"기술 블로그","link":"/techblogs/address-structure-3.html"},{"title":"모바일 지도 SDK의 재설계","description":"C++ 통합 코어와 그래픽 추상화 아키텍처로 재설계한 카카오내비 신규 지도 SDK(KNMSDK)의 설계 배경과 성능 개선 성과를 소개합니다.","date":"2026.01.28","author":"제이지(성학종), 루크(유태훈), 에단(김병주)","image":"https://t1.kakaocdn.net/km_developers/docs/techblogs/knmsdk-mapv2/knmsdk-mapv2_thumbnail.png","category":"기술 블로그","link":"/techblogs/knmsdk-mapv2.html"}]`);
            export{t as d};
            """.trimIndent()
        }

        posts.size shouldBe 2
        posts[0].key shouldBe "techblogs/address-structure-3.html"
        posts[0].title shouldBe "도로명주소와 건축물·토지 데이터 레이어링으로 도출하는 공간 인사이트"
        posts[0].description shouldBe "도로명주소와 지번주소를 매개로 건축물대장과 토지특성도 데이터를 결합하여, 건물의 입체적 프로필과 땅의 물리적 환경을 분석하고 모빌리티 서비스의 비즈니스 가치를 창출하는 기술적 로직을 공유합니다."
        posts[0].tags shouldBe listOf("기술 블로그")
        posts[0].thumbnail shouldBe "https://t1.kakaocdn.net/km_developers/docs/techblogs/address-structure-3/thumbnail.png"
        posts[0].publishedAt shouldBe LocalDate.of(2026, 4, 8).atStartOfDay()
        posts[0].url shouldBe "https://developers.kakaomobility.com/techblogs/address-structure-3.html"
    }

    @Test
    fun `데이터 번들이 없으면 SSR HTML 카드 구조를 파싱한다`() {
        val doc = Jsoup.parse(
            """
            <main>
              <a href="/techblogs/address-structure-3.html" class="new-content-card">
                <img src="https://t1.kakaocdn.net/km_developers/docs/techblogs/address-structure-3/thumbnail.png" alt="thumbnail">
                <h3 class="new-content-card-title">도로명주소와 건축물·토지 데이터 레이어링으로 도출하는 공간 인사이트</h3>
                <span class="new-content-card-time">2026.04.08</span>
                <span class="new-content-card-author">지미(박지민)</span>
              </a>
              <a href="/techblogs/address-structure-2.html" class="blog-card">
                <img src="https://t1.kakaocdn.net/km_developers/docs/techblogs/address-structure-2/012_thumbnail.png" alt="thumbnail">
                <strong class="blog-card__title">주소 데이터 활용을 위한 지번주소와 도로명주소 주소 체계 이해하기</strong>
                <div class="blog-card__description">우리나라 주소 체계의 기본 개념과 데이터 활용 방법을 정리합니다.</div>
                <span class="blog-card__meta">2025.10.15 | 지미(박지민)</span>
              </a>
            </main>
            """.trimIndent(),
            "https://developers.kakaomobility.com/techblogs/"
        )

        val posts = source.parseItems(doc)

        posts.size shouldBe 2
        posts[0].key shouldBe "techblogs/address-structure-3.html"
        posts[0].description shouldBe ""
        posts[0].publishedAt shouldBe LocalDate.of(2026, 4, 8).atStartOfDay()
        posts[1].key shouldBe "techblogs/address-structure-2.html"
        posts[1].description shouldBe "우리나라 주소 체계의 기본 개념과 데이터 활용 방법을 정리합니다."
        posts[1].publishedAt shouldBe LocalDate.of(2025, 10, 15).atStartOfDay()
    }
}
