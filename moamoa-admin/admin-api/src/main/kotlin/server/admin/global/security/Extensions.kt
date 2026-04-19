package server.admin.global.security

import server.core.feature.member.domain.MemberRole

internal fun AdminPassport.ensureAdmin() {
    if (role != MemberRole.ADMIN) {
        throw AdminForbiddenException()
    }
}
