package server.application

import org.springframework.stereotype.Component
import server.infra.cache.CacheMemory
import server.infra.cache.get

@Component
class EmailVerificationCache(
    private val cacheMemory: CacheMemory
) {

    private val verificationCodePrefix = "VERIFICATION_CODE:"
    private val emailVerifiedPrefix = "EMAIL_VERIFIED:"

    private val fiveMinutes = 300_000L
    private val tenMinutes = 600_000L

    private fun verificationCodeKey(email: String): String = verificationCodePrefix + email

    suspend fun setVerificationCode(email: String, verificationCode: String) {
        cacheMemory.set(
            key = verificationCodeKey(email),
            value = verificationCode,
            ttlMillis = fiveMinutes
        )
    }

    suspend fun getVerificationCode(email: String): String? {
        return cacheMemory.get<String>(verificationCodeKey(email))
    }

    private fun verifiedKey(email: String): String = emailVerifiedPrefix + email

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