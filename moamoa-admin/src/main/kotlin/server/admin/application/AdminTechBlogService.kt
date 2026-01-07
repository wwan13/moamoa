package server.admin.application

import org.springframework.stereotype.Service
import server.admin.domain.techblog.AdminTechBlog
import server.admin.domain.techblog.AdminTechBlogRepository
import server.admin.infra.db.AdminTransactional
import server.techblog.TechBlogSources

@Service
internal class AdminTechBlogService(
    private val transactional: AdminTransactional,
    private val techBlogRepository: AdminTechBlogRepository,
    private val techBlogSources: TechBlogSources
) {

    suspend fun create(command: AdminCreateTechBlogCommand): AdminTechBlogData = transactional {
        validateTitle(command.title)
        val techBlog = AdminTechBlog(
            title = command.title,
            icon = command.icon,
            blogUrl = command.blogUrl,
            key = command.key
        )
        techBlogSources.validateExists(techBlog.key)
        techBlogRepository.save(techBlog).let(::AdminTechBlogData)
    }

    suspend fun update(
        id: Long,
        command: AdminUpdateTechBlogCommand
    ): AdminTechBlogData = transactional {
        validateTitle(command.title)
        val techBlog = techBlogRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 tech blog 입니다.")
        techBlog.update(command.title, command.blogUrl, command.icon)
        techBlogRepository.save(techBlog).let(::AdminTechBlogData)
    }

    private suspend  fun validateTitle(title: String) {
        if (techBlogRepository.existsByTitle(title)) {
            throw IllegalArgumentException("이미 존재하는 tech blog 입니다.")
        }
    }
}