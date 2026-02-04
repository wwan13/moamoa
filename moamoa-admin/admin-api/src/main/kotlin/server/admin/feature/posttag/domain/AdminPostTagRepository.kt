package server.admin.feature.posttag.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

internal interface AdminPostTagRepository : CoroutineCrudRepository<AdminPostTag, Long>{
}
