package server.application

import org.springframework.stereotype.Service
import server.domain.tag.TagRepository

@Service
class TagService(
    private val tagRepository: TagRepository
) {

    suspend fun findAll(): List<TagData> {
        return tagRepository.findAllByOrderByTitleAsc().map(::TagData)
    }
}