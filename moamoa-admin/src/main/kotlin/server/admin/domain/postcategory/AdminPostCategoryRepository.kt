package server.admin.domain.postcategory

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPostCategoryRepository : CoroutineCrudRepository<AdminPostCategory, Long>{
}