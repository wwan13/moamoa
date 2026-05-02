package server.admin.feature.techblog.infra

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import server.techblog.TechBlogPost
import server.techblog.TechBlogSources

@Component
internal class TechBlogCollector(
    private val techBlogSources: TechBlogSources,
) {
    fun collect(techBlogKey: String): List<TechBlogPost> {
        return runBlocking {
            techBlogSources[techBlogKey].getPosts().toList()
        }.distinctBy { it.key }
    }

    fun validateExists(techBlogKey: String) {
        techBlogSources.validateExists(techBlogKey)
    }
}
