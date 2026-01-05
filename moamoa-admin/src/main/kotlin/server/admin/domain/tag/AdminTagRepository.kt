package server.admin.domain.tag

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminTagRepository : CoroutineCrudRepository<AdminTag, Long> {
    suspend fun findAllByTitleIn(titles: List<String>): List<AdminTag>
}