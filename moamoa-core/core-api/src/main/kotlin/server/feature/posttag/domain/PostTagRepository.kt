package server.feature.posttag.domain

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PostTagRepository : CoroutineCrudRepository<PostTag, Long> {
}