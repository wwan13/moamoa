package server.admin.application

import org.springframework.stereotype.Component
import server.client.techblogs.TechBlogClient

@Component
class AdminTechBlogClients(
    private val techBlogClients: Map<String, TechBlogClient>
) {
    fun get(key: String): TechBlogClient {
        return techBlogClients[key]
            ?: throw IllegalArgumentException("존재하지 않는 tech blog client 입니다.")
    }

    fun exists(key: String) = key in techBlogClients

    fun validateExists(key: String) {
        if (!exists(key)) {
            throw IllegalArgumentException("tech blog client가 존재하지 않습니다.")
        }
    }
}