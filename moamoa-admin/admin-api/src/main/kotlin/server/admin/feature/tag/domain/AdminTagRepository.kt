package server.admin.feature.tag.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminTagRepository : CoroutineCrudRepository<AdminTag, Long> {
    suspend fun findAllByTitleIn(titles: List<String>): List<AdminTag>
}
