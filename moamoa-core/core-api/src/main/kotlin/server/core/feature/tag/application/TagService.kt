package server.core.feature.tag.application

import org.springframework.stereotype.Service
import server.core.feature.tag.domain.TagRepository

@Service
class TagService(
    private val tagRepository: TagRepository
) {

    fun findAll(): List<TagData> {
        return tagRepository.findAllByOrderByTitleAsc().map(::TagData)
    }
}