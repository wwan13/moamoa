package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.application.CreateMemberCommand
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
        @RequestBody command: CreateMemberCommand
    ): ResponseEntity<MemberData> {
        val response = memberService.createMember(command)
        val uri = "/api/member/${response.id}".toUri()

        return ResponseEntity.created(uri).body(response)
    }

    @GetMapping("/email-exists")
    suspend fun emailExists(
        @RequestParam email: String
    ): ResponseEntity<EmailExistsResult> {
        val exists = memberService.emailExists(email)
        val response = EmailExistsResult(exists)

        return ResponseEntity.ok(response)
    }
}