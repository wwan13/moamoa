package server.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import server.domain.techblog.TechBlogRepository

@Service
class TechBlogService(
    private val transactional: Transactional,
    private val techBlogRepository: TechBlogRepository
) {
    fun findById(id: Long): TechBlogData = transactional(readOnly = true) {
        techBlogRepository.findByIdOrNull(id)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하는 tech blog가 아닙니다.")
    }

    fun findAll(): List<TechBlogData> = transactional(readOnly = true) {
        techBlogRepository.findAll().map(::TechBlogData)
    }
}
