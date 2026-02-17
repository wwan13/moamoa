package server.infra.cache

import org.springframework.stereotype.Component
import server.shared.cache.CacheMemory
import server.shared.cache.get

@Component
class EmailVerificationCache(
    private val cacheMemory: CacheMemory
) {

    private val verificationCodePrefix = "VERIFICATION_CODE:"
    private val emailVerifiedPrefix = "EMAIL_VERIFIED:"

    private val fiveMinutes = 300_000L
    private val tenMinutes = 600_000L

    private fun verificationCodeKey(email: String): String =
        verificationCodePrefix + email

    private fun verifiedKey(email: String): String =
        emailVerifiedPrefix + email

    suspend fun setVerificationCode(email: String, verificationCode: String) {
        cacheMemory.set(
            key = verificationCodeKey(email),
            value = verificationCode,
            ttlMillis = fiveMinutes
        )
    }

    suspend fun getVerificationCode(email: String): String? {
        return cacheMemory.get(verificationCodeKey(email))
    }

    suspend fun setVerified(email: String) {
        cacheMemory.set(
            key = verifiedKey(email),
            value = true,
            ttlMillis = tenMinutes
        )
    }

    suspend fun isVerified(email: String): Boolean {
        return cacheMemory.get<Boolean>(verifiedKey(email)) != null
    }
}
