package server.techblog.http

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource

internal class PythonCrawlerTechBlogSource(
    private val client: TechBlogCrawlerClient,
    properties: TechBlogCrawlerProperties,
) : TechBlogSource {

    private val supportedKeys = properties.supportedKeys.map { it.lowercase() }.toSet()

    override suspend fun getPosts(key: String, size: Int?): Flow<TechBlogPost> {
        require(exists(key)) { "tech blog source가 존재하지 않습니다." }
        return client.crawl(key = key, size = size).posts.asFlow()
    }

    override fun exists(key: String): Boolean = key.lowercase() in supportedKeys
}
