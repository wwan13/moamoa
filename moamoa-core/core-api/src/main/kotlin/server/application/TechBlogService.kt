package server.application

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.domain.techblog.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    suspend fun findById(techBlogId: Long): TechBlogData {
        return techBlogRepository.findById(techBlogId)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
    }

    suspend fun findAll(): List<TechBlogData> {
        return techBlogRepository.findAll().map(::TechBlogData).toList()
    }
}
