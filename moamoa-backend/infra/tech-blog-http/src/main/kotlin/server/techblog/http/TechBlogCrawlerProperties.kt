package server.techblog.http

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "tech-blog.crawler")
internal data class TechBlogCrawlerProperties(
    val baseUrl: String = "http://127.0.0.1:8765",
    val connectTimeout: Duration = Duration.ofSeconds(30),
    val responseTimeout: Duration = Duration.ofMinutes(15),
    val maxInMemorySize: Int = 50 * 1024 * 1024,
    val supportedKeys: List<String> = DEFAULT_SUPPORTED_KEYS,
) {

    companion object {
        private val DEFAULT_SUPPORTED_KEYS = listOf(
            "ab180",
            "ably",
            "banksalad",
            "buzzvil",
            "com2us",
            "daangn",
            "danawa",
            "delightroom",
            "devocean",
            "elevenst",
            "flex",
            "gabia",
            "gccompany",
            "goorm",
            "hyperconnect",
            "kakao",
            "kakaobank",
            "kakaomobility",
            "kakaopay",
            "kakaostyle",
            "kream",
            "ktcloud",
            "kurly",
            "kyobodts",
            "line",
            "lotteon",
            "miridih",
            "musinsa",
            "myrealtrip",
            "naver",
            "naverplace",
            "nds",
            "nhncloud",
            "oliveyoung",
            "postype",
            "rapportlabs",
            "remember",
            "samosam",
            "samsung",
            "saramin",
            "skplanet",
            "socar",
            "ssgtech",
            "tabling",
            "tmapmobility",
            "toss",
            "wanted",
            "watcha",
            "woowabros",
            "yogiyo",
        )
    }
}
