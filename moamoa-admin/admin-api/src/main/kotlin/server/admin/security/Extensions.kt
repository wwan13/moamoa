package server.admin.security

import server.admin.feature.member.domain.AdminMemberRole

internal fun AdminPassport.ensureAdmin() {
    if (role != AdminMemberRole.ADMIN) {
        throw AdminForbiddenException()
    }
}