package server.domain.tag

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TagRepository : CoroutineCrudRepository<Tag, Long> {
    suspend fun findAllByOrderByTitleAsc(): List<Tag>
}