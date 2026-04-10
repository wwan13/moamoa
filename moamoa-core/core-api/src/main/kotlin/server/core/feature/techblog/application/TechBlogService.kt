package server.core.feature.techblog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.techblog.domain.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    @Transactional(readOnly = true)
    fun findByKey(techBlogKey: String): TechBlogData {
        return techBlogRepository.findByKey(techBlogKey)?.let(::TechBlogData)
            ?: throw NoSuchElementException("존재하지 않는 기술 블로그 입니다.")
    }

    @Transactional(readOnly = true)
    fun findAll(): List<TechBlogData> {
        return techBlogRepository.findAllByOrderByTitleAsc().map(::TechBlogData)
    }
}
