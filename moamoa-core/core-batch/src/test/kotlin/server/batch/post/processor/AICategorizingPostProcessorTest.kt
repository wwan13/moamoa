package server.batch.post.processor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import server.batch.post.dto.PostSummary
import server.shared.ai.ChatCompletion
import server.shared.ai.Prompt
import test.UnitTest

class AICategorizingPostProcessorTest : UnitTest() {

    @Test
    fun `AI 분류 프롬프트는 신규 카테고리 ID 체계를 사용한다`() {
        val chatCompletion = mockk<ChatCompletion>()
        val promptsSlot = slot<List<Prompt>>()
        val sut = AICategorizingPostProcessor(chatCompletion, jacksonObjectMapper())
        coEvery { chatCompletion.invoke(capture(promptsSlot), temperature = 0.1) } returns "[]"

        sut.process(
            listOf(
                PostSummary(
                    postId = 101L,
                    title = "spring post",
                    description = "backend",
                    key = "p-101",
                    tags = listOf("spring")
                )
            )
        )

        val systemPrompt = promptsSlot.captured.first { it.role == "system" }.message

        systemPrompt.shouldContain("- 10: ENGINEERING")
        systemPrompt.shouldContain("- 20: PRODUCT")
        systemPrompt.shouldContain("- 30: DESIGN")
        systemPrompt.shouldContain("- 40: ETC")
        systemPrompt.shouldContain("ETC(40)")

        systemPrompt.shouldNotContain("- 1: BACKEND")
        systemPrompt.shouldNotContain("- 2: FRONTEND")
        systemPrompt.shouldNotContain("- 3: PRODUCT")
        systemPrompt.shouldNotContain("- 4: DESIGN")
        systemPrompt.shouldNotContain("- 6: ETC")
    }
}
