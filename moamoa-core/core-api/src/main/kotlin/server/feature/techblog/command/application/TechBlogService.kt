package server.feature.techblog.command.application

import org.springframework.stereotype.Service
import server.feature.techblog.command.domain.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    fun findByKey(techBlogKey: String): TechBlogData {
        return techBlogRepository.findByKey(techBlogKey)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
    }

    fun findAll(): List<TechBlogData> {
        return techBlogRepository.findAllByOrderByTitleAsc().map(::TechBlogData)
    }
}
