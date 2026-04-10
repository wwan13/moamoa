package server.core.feature.tag.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.tag.domain.TagRepository

@Service
class TagService(
    private val tagRepository: TagRepository
) {

    @Transactional(readOnly = true)
    fun findAll(): List<TagData> {
        return tagRepository.findAllByOrderByTitleAsc().map(::TagData)
    }
}
