package server.admin.presentation

import kotlinx.coroutines.flow.toList
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import server.techblog.TechBlogSources

@RestController
class AdminTestController(
    private val techBlogSources: TechBlogSources
) {

    @GetMapping("/admin/test/{key}")
    suspend fun test(
        @PathVariable key: String
    ): String {
        val result = techBlogSources[key].getPosts().toList()
        println(result)
        return result.toString()
    }
}