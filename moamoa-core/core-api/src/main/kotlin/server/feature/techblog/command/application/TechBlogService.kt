package server.feature.techblog.command.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import server.feature.techblog.command.domain.TechBlogRepository

@Service
class TechBlogService(
    private val techBlogRepository: TechBlogRepository
) {
    suspend fun findByKey(techBlogKey: String): TechBlogData {
        return techBlogRepository.findByKey(techBlogKey)?.let(::TechBlogData)
            ?: throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
    }

    suspend fun findAll(): Flow<TechBlogData> {
        return techBlogRepository.findAllByOrderByTitleAsc().map(::TechBlogData)
    }
}
