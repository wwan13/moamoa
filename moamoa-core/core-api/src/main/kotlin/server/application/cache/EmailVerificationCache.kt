package server.application.cache

interface EmailVerificationCache {

    suspend fun setVerificationCode(email: String, verificationCode: String)

    suspend fun getVerificationCode(email: String): String?

    suspend fun setVerified(email: String)

    suspend fun isVerified(email: String): Boolean
}