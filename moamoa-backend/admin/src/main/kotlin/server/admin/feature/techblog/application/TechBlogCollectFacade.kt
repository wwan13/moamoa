package server.admin.feature.techblog.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.admin.feature.techblog.domain.AdminTechBlogRepository
import server.admin.feature.techblog.infra.TechBlogCollector

@Service
internal class TechBlogCollectFacade(
    private val techBlogRepository: AdminTechBlogRepository,
    private val techBlogCollector: TechBlogCollector,
    private val techBlogService: AdminTechBlogService,
) {
    fun collectPosts(command: AdminCollectPostsCommand): AdminCollectPostsResult {
        val techBlog = techBlogRepository.findByIdOrNull(command.techBlogId)
            ?: throw NoSuchElementException("존재하지 않는 tech blog 입니다.")

        val fetchedPosts = techBlogCollector.collect(techBlog.key)
        return techBlogService.createPosts(command.techBlogId, fetchedPosts)
    }
}
