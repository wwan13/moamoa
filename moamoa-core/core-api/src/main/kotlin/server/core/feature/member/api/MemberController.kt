package server.core.feature.member.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import server.core.feature.member.application.ChangePasswordCommand
import server.core.feature.member.application.CreateInternalMemberCommand
import server.core.feature.member.application.CreateSocialMemberCommand
import server.core.feature.member.application.CreateSocialMemberResult
import server.core.feature.member.application.EmailExistsCommand
import server.core.feature.member.application.EmailExistsResult
import server.core.feature.member.application.MemberData
import server.core.feature.member.application.MemberService
import server.core.feature.member.application.MemberUnjoinService
import server.core.feature.member.query.MemberQueryService
import server.core.feature.member.query.MemberSummary
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.web.ApiResponse

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
    ): ApiResponse<MemberData> {
        val response = memberService.createInternalMember(command)

        return ApiResponse.of(response)
    }

    @PostMapping("/social")
    fun createSocialMember(
        @RequestBody @Valid command: CreateSocialMemberCommand
    ): ApiResponse<CreateSocialMemberResult> {
        val response = memberService.createSocialMemberWithSession(command)

        return ApiResponse.of(response)
    }

    @GetMapping("/email-exists")
    fun emailExists(
        @Valid command: EmailExistsCommand
    ): ApiResponse<EmailExistsResult> {
        val response = memberService.emailExists(command)

        return ApiResponse.of(response)
    }

    @GetMapping
    fun findByPassport(
        @RequestPassport passport: Passport
    ): ApiResponse<MemberSummary> {
        val response = memberQueryService.findById(passport.memberId)

        return ApiResponse.of(response)
    }

    @PostMapping("/password")
    fun changePassword(
        @RequestBody @Valid command: ChangePasswordCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        memberService.changePassword(command, passport)

        return ApiResponse.of()
    }

    @DeleteMapping
    fun unjoin(
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        memberUnjoinService.unjoin(passport)

        return ApiResponse.of()
    }
}
