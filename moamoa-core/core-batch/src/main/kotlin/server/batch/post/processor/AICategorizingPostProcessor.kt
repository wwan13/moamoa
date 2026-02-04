package server.batch.post.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.batch.post.dto.PostCategory
import server.batch.post.dto.PostSummary
import server.chat.ChatCompletion
import server.chat.Prompt

@Component
internal class AICategorizingPostProcessor(
    private val chatCompletion: ChatCompletion,
    private val objectMapper: ObjectMapper
) : ItemProcessor<List<PostSummary>, List<PostCategory>> {

    override fun process(items: List<PostSummary>): List<PostCategory>? = runBlocking {
        try {
            val raw = chatCompletion.invoke(prompts(items), temperature = 0.1)
            objectMapper.readValue(raw)
        } catch (e: Exception) {
            println(e)
            null
        }
    }

    private fun prompts(summaries: List<PostSummary>): List<Prompt> = listOf(
        Prompt.system(
            """
                너는 기술 블로그 게시글을 카테고리로 분류하는 분류기다.
            
                입력 형식:
                - 입력은 JSON 배열이다.
                - 각 원소는 아래 구조를 가진다.
                  {
                    "postId": number,
                    "title": string,
                    "description": string,
                    "key": string,
                    "tags": string[]
                  }
            
                출력 형식:
                - 반드시 JSON 배열만 출력한다.
                - 배열 길이는 입력 배열 길이와 반드시 동일해야 한다.
                - 각 원소는 아래 형식을 따른다.
                  {
                    "postId": number,
                    "categoryId": number
                  }
            
                카테고리 ID:
                - 1: BACKEND
                - 2: FRONTEND
                - 3: PRODUCT
                - 4: DESIGN
                - 6: ETC
            
                분류 전략(매우 중요):
                - 너가 보기에 가장 유사한 카테고리로 선정 해줘
                - 저기에 포함되지 않는 경우 ETC(6)으로 보내줘
            
                금지 사항:
                - 설명, 주석, 코드블록, 마크다운을 출력하지 않는다.
                - JSON 외의 텍스트를 절대 출력하지 않는다.
            
                출력 예시:
                [
                  {"postId": 101, "categoryId": 1},
                  {"postId": 102, "categoryId": 3}
                ]
                """.trimIndent()
        ),
        Prompt.user(objectMapper.writeValueAsString(summaries))
    )
}
