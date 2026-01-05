package server.admin.domain.posttag

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPostTagRepository : CoroutineCrudRepository<AdminPostTag, Long>{
}