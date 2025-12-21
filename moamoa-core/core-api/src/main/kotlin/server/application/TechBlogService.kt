package server.application

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.domain.techblog.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    suspend fun findById(id: Long): TechBlogData {
        return techBlogRepository.findById(id)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하는 tech blog가 아닙니다.")
    }

    suspend fun findAll(): List<TechBlogData> {
        return techBlogRepository.findAll().map(::TechBlogData).toList()
    }
}
