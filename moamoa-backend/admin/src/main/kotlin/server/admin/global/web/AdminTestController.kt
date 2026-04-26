package server.admin.global.web

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import server.techblog.TechBlogSources

@RestController
internal class AdminTestController(
    private val techBlogSources: TechBlogSources
) {

    @GetMapping("/admin/test/{key}")
    fun test(
        @PathVariable key: String
    ): String {
        val result = runBlocking { techBlogSources[key].getPosts().toList() }
        println(result)
        println(result.size)
        return result.toString()
    }
}
