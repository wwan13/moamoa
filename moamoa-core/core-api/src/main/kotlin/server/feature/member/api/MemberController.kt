package server.feature.member.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.member.application.CreateMemberCommand
import server.feature.member.application.EmailExistsCommand
import server.feature.member.application.EmailExistsResult
import server.feature.member.application.MemberData
import server.feature.member.application.MemberService
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