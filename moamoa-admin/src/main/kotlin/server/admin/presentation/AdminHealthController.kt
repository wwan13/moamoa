package server.admin.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminHealthController {

    @GetMapping("/admin/health-check")
    fun healthCheck(): ResponseEntity<String> {
        return ResponseEntity.ok("healthy")
    }
}