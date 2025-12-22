package server.infra.cache

import org.springframework.stereotype.Component
import server.application.cache.EmailVerificationCache

@Component
class RedisEmailVerificationCache(
    private val cacheMemory: CacheMemory
) : EmailVerificationCache {

    private val verificationCodePrefix = "VERIFICATION_CODE:"
    private val emailVerifiedPrefix = "EMAIL_VERIFIED:"

    private val fiveMinutes = 300_000L
    private val tenMinutes = 600_000L

    private fun verificationCodeKey(email: String): String =
        verificationCodePrefix + email

    private fun verifiedKey(email: String): String =
        emailVerifiedPrefix + email

    override suspend fun setVerificationCode(email: String, verificationCode: String) {
        cacheMemory.set(
            key = verificationCodeKey(email),
            value = verificationCode,
            ttlMillis = fiveMinutes
        )
    }

    override suspend fun getVerificationCode(email: String): String? {
        return cacheMemory.get(verificationCodeKey(email))
    }

    override suspend fun setVerified(email: String) {
        cacheMemory.set(
            key = verifiedKey(email),
            value = true,
            ttlMillis = tenMinutes
        )
    }

    override suspend fun isVerified(email: String): Boolean {
        // 존재 여부만 확인
        return cacheMemory.get<Boolean>(verifiedKey(email)) != null
    }
}