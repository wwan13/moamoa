package server.feature.tag.application

import org.springframework.stereotype.Service
import server.feature.tag.domain.TagRepository

@Service
class TagService(
    private val tagRepository: TagRepository
) {

    suspend fun findAll(): List<TagData> {
        return tagRepository.findAllByOrderByTitleAsc().map(::TagData)
    }
}