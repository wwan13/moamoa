package server.feature.member.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.member.command.application.CreateInternalMemberCommand
import server.feature.member.command.application.CreateSocialMemberCommand
import server.feature.member.command.application.CreateSocialMemberResult
import server.feature.member.command.application.EmailExistsCommand
import server.feature.member.command.application.EmailExistsResult
import server.feature.member.command.application.MemberData
import server.feature.member.command.application.MemberService
import server.feature.member.query.MemberQueryService
import server.feature.member.query.MemberSummary
import server.security.Passport
import server.security.RequestPassport
import support.uri.toUri

@RestController
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService,
    private val memberQueryService: MemberQueryService,
) {

    @PostMapping
    suspend fun createInternalMember(
        @RequestBody @Valid command: CreateInternalMemberCommand
    ): ResponseEntity<MemberData> {
        val response = memberService.createInternalMember(command)
        val uri = "/api/member/${response.id}".toUri()

        return ResponseEntity.created(uri).body(response)
    }

    @PostMapping("/social")
    suspend fun createSocialMember(
        @RequestBody @Valid command: CreateSocialMemberCommand
    ): ResponseEntity<CreateSocialMemberResult> {
        val response = memberService.createSocialMemberWithSession(command)
        val uri = "/api/member/${response.member.id}".toUri()

        return ResponseEntity.created(uri).body(response)
    }

    @GetMapping("/email-exists")
    suspend fun emailExists(
        @Valid command: EmailExistsCommand
    ): ResponseEntity<EmailExistsResult> {
        val response = memberService.emailExists(command)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    suspend fun findByPassport(
        @RequestPassport passport: Passport
    ): ResponseEntity<MemberSummary> {
        val response = memberQueryService.findById(passport.memberId)

        return ResponseEntity.ok(response)
    }
}