package server.admin.feature.techblog.infra

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSource

@Component
internal class TechBlogCollector(
    private val techBlogSource: TechBlogSource,
) {
    fun collect(techBlogKey: String): List<TechBlogPost> {
        return runBlocking {
            techBlogSource.getPosts(techBlogKey).toList()
        }.distinctBy { it.key }
    }

    fun validateExists(techBlogKey: String) {
        if (!techBlogSource.exists(techBlogKey)) {
            throw IllegalArgumentException("tech blog source가 존재하지 않습니다.")
        }
    }
}
