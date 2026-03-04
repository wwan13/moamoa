package server.feature.tag.domain

import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findAllByOrderByTitleAsc(): List<Tag>
}