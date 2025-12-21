package server.admin.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import server.admin.application.AdminTechBlogClients

@RestController
class AdminTestController(
    private val adminTechBlogClients: AdminTechBlogClients
) {

    @GetMapping("/admin/test/{key}")
    suspend fun test(
        @PathVariable key: String
    ): String {
        val result = adminTechBlogClients.get(key + "Client").getPosts()
        println(result)
        return result.toString()
    }
}