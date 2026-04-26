package server.admin.global.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
internal class AdminHealthController {

    @GetMapping("/admin/health-check")
    fun healthCheck(): AdminApiResponse<String> {
        return AdminApiResponse.of("healthy")
    }
}
