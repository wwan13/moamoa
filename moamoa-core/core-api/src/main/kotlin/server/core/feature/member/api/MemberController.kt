package server.core.feature.member.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import server.core.feature.member.application.ChangePasswordCommand
import server.core.feature.member.application.ChangePasswordResult
import server.core.feature.member.application.CreateInternalMemberCommand
import server.core.feature.member.application.CreateSocialMemberCommand
import server.core.feature.member.application.CreateSocialMemberResult
import server.core.feature.member.application.EmailExistsCommand
import server.core.feature.member.application.EmailExistsResult
import server.core.feature.member.application.MemberData
import server.core.feature.member.application.MemberService
import server.core.feature.member.application.MemberUnjoinResult
import server.core.feature.member.application.MemberUnjoinService
import server.core.feature.member.query.MemberQueryService
import server.core.feature.member.query.MemberSummary
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/member")
class MemberController(
    private val memberService: MemberService,
    private val memberUnjoinService: MemberUnjoinService,
    private val memberQueryService: MemberQueryService,
) {

    @PostMapping
    fun createInternalMember(
        @RequestBody @Valid command: CreateInternalMemberCommand
    ): ResponseEntity<MemberData> {
        val response = memberService.createInternalMember(command)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/social")
    fun createSocialMember(
        @RequestBody @Valid command: CreateSocialMemberCommand
    ): ResponseEntity<CreateSocialMemberResult> {
        val response = memberService.createSocialMemberWithSession(command)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/email-exists")
    fun emailExists(
        @Valid command: EmailExistsCommand
    ): ResponseEntity<EmailExistsResult> {
        val response = memberService.emailExists(command)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun findByPassport(
        @RequestPassport passport: Passport
    ): ResponseEntity<MemberSummary> {
        val response = memberQueryService.findById(passport.memberId)

        return ResponseEntity.ok(response)
    }

    @PatchMapping("/password")
    fun changePassword(
        @RequestBody @Valid command: ChangePasswordCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<ChangePasswordResult> {
        val response = memberService.changePassword(command, passport)

        return ResponseEntity.ok(response)
    }

    @DeleteMapping
    fun unjoin(
        @RequestPassport passport: Passport
    ): ResponseEntity<MemberUnjoinResult> {
        val response = memberUnjoinService.unjoin(passport)

        return ResponseEntity.ok(response)
    }
}