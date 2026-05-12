package server.admin.global.web

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import server.techblog.TechBlogSource

@RestController
internal class AdminTestController(
    private val techBlogSource: TechBlogSource
) {

    @GetMapping("/admin/test/{key}")
    fun test(
        @PathVariable key: String
    ): String {
        val result = runBlocking { techBlogSource.getPosts(key).toList() }
        println(result)
        println(result.size)
        return result.toString()
    }
}
