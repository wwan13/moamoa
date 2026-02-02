package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import test.UnitTest

class EmailVerificationCacheTest : UnitTest() {
    @Test
    fun `인증 코드를 캐시에 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = EmailVerificationCache(cacheMemory)
        val email = "user@example.com"
        val key = "VERIFICATION_CODE:$email"

        coEvery { cacheMemory.set(key, "code", 300_000L) } just Runs

        cache.setVerificationCode(email, "code")

        coVerify(exactly = 1) { cacheMemory.set(key, "code", 300_000L) }
    }

    @Test
    fun `인증 코드를 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = EmailVerificationCache(cacheMemory)
        val email = "user@example.com"
        val key = "VERIFICATION_CODE:$email"

        coEvery { cacheMemory.get(key, any<TypeReference<String>>()) } returns "code"

        val result = cache.getVerificationCode(email)

        result shouldBe "code"
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<String>>()) }
    }

    @Test
    fun `인증 완료 상태를 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = EmailVerificationCache(cacheMemory)
        val email = "user@example.com"
        val key = "EMAIL_VERIFIED:$email"

        coEvery { cacheMemory.set(key, true, 600_000L) } just Runs

        cache.setVerified(email)

        coVerify(exactly = 1) { cacheMemory.set(key, true, 600_000L) }
    }

    @Test
    fun `인증 완료 상태가 존재하면 true를 반환한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = EmailVerificationCache(cacheMemory)
        val email = "user@example.com"
        val key = "EMAIL_VERIFIED:$email"

        coEvery { cacheMemory.get(key, any<TypeReference<Boolean>>()) } returns true

        val result = cache.isVerified(email)

        result shouldBe true
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<Boolean>>()) }
    }

    @Test
    fun `인증 완료 상태가 없으면 false를 반환한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = EmailVerificationCache(cacheMemory)
        val email = "user@example.com"
        val key = "EMAIL_VERIFIED:$email"

        coEvery { cacheMemory.get(key, any<TypeReference<Boolean>>()) } returns null

        val result = cache.isVerified(email)

        result shouldBe false
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<Boolean>>()) }
    }
}
