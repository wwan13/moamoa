package server.admin.domain.posttag

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminPostTagRepository : CoroutineCrudRepository<AdminPostTag, Long>{
}