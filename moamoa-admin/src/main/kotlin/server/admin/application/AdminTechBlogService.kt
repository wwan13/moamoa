package server.admin.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.admin.domain.techblog.AdminTechBlog
import server.admin.domain.techblog.AdminTechBlogRepository
import server.client.techblogs.TechBlogClient

@Service
class AdminTechBlogService(
    private val techBlogRepository: AdminTechBlogRepository,
    private val techBlogClients: Map<String, TechBlogClient>
) {

    @Transactional
    fun create(command: AdminCreateTechBlogCommand): AdminTechBlogData {
        validateTitle(command.title)
        val techBlog = AdminTechBlog(
            title = command.title,
            icon = command.icon,
            blogUrl = command.blogUrl,
            key = command.key
        )
        if (techBlog.clientBeanName !in techBlogClients) {
            throw IllegalArgumentException("client class를 먼저 구현해 주세요.")
        }
        return techBlogRepository.save(techBlog).let(::AdminTechBlogData)
    }

    @Transactional
    fun update(
        id: Long,
        command: AdminUpdateTechBlogCommand
    ): AdminTechBlogData {
        validateTitle(command.title)
        val techBlog = techBlogRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("존재하지 않는 tech blog 입니다.")
        techBlog.update(command.title, command.blogUrl, command.icon)
        return techBlogRepository.save(techBlog).let(::AdminTechBlogData)
    }

    private fun validateTitle(title: String) {
        if (techBlogRepository.existsByTitle(title)) {
            throw IllegalArgumentException("이미 존재하는 tech blog 입니다.")
        }
    }
}