package server.core.global.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/health-check")
    fun healthCheck(): ApiResponse<String> {
        return ApiResponse.of("healthy")
    }
}
