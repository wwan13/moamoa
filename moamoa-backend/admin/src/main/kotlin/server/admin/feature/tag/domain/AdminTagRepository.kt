package server.admin.feature.tag.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminTagRepository : JpaRepository<AdminTag, Long> {
    fun findAllByTitleIn(titles: List<String>): List<AdminTag>
}
