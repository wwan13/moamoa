package server.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.domain.techblog.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    @Transactional(readOnly = true)
    fun findById(id: Long): TechBlogData {
        return techBlogRepository.findByIdOrNull(id)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하는 tech blog가 아닙니다.")
    }

    @Transactional(readOnly = true)
    fun findAll(): List<TechBlogData> {
        return techBlogRepository.findAll().map(::TechBlogData)
    }
}