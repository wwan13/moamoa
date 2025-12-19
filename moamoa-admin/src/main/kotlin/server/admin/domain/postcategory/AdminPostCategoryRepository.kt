package server.admin.domain.postcategory

import org.springframework.data.jpa.repository.JpaRepository

interface AdminPostCategoryRepository : JpaRepository<AdminPostCategory, Long>{
}