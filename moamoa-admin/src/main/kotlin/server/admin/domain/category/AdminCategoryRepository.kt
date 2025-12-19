package server.admin.domain.category

import org.springframework.data.jpa.repository.JpaRepository

interface AdminCategoryRepository : JpaRepository<AdminCategory, Long> {
    fun findAllByTitleIn(titles: List<String>): List<AdminCategory>
}