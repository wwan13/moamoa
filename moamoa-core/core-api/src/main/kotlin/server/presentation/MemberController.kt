package server.presentation

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.application.CreateMemberCommand
import server.application.EmailExistsCommand
import server.application.EmailExistsResult
import server.application.MemberData
import server.application.MemberService
import support.uri.toUri

@RestController
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService
) {

    @PostMapping
    suspend fun createMember(
        @RequestBody @Valid command: CreateMemberCommand
    ): ResponseEntity<MemberData> {
        val response = memberService.createMember(command)
        val uri = "/api/member/${response.id}".toUri()

        return ResponseEntity.created(uri).body(response)
    }

    @GetMapping("/email-exists")
    suspend fun emailExists(
        @Valid command: EmailExistsCommand
    ): ResponseEntity<EmailExistsResult> {
        val response = memberService.emailExists(command)

        return ResponseEntity.ok(response)
    }
}