package server.admin.global.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class AdminHealthController {

    @GetMapping("/admin/health-check")
    suspend fun healthCheck(): ResponseEntity<String> {
        return ResponseEntity.ok("healthy")
    }
}
