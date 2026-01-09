package server.feature.tag.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TagRepository : CoroutineCrudRepository<Tag, Long> {
    suspend fun findAllByOrderByTitleAsc(): List<Tag>
}