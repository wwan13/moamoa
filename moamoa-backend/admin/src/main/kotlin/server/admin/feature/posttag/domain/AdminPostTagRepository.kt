package server.admin.feature.posttag.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminPostTagRepository : JpaRepository<AdminPostTag, Long> {
    fun findAllByPostIdIn(postIds: Collection<Long>): List<AdminPostTag>
}
