package server.core.feature.notice.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.notice.application.NoticeData
import server.core.feature.notice.application.NoticeService
import server.core.feature.notice.query.NoticeList
import server.core.feature.notice.query.NoticeQueryConditions
import server.core.feature.notice.query.NoticeQueryService
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/notice")
class NoticeController(
    private val noticeService: NoticeService,
    private val noticeQueryService: NoticeQueryService,
){

    @GetMapping
    fun findByConditions(
        conditions: NoticeQueryConditions,
    ): ApiResponse<NoticeList> {
        val response = noticeQueryService.findByConditions(conditions)

        return ApiResponse.of(response)
    }

    @GetMapping("/{noticeId}")
    fun findById(
        @PathVariable noticeId: Long,
    ): ApiResponse<NoticeData> {
        val response = noticeService.findById(noticeId)

        return ApiResponse.of(response)
    }
}
