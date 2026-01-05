package server.domain.posttag

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PostTagRepository : CoroutineCrudRepository<PostTag, Long> {
}