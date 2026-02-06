package server.batch.techblog.reader

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider
import org.springframework.test.util.ReflectionTestUtils
import test.UnitTest
import javax.sql.DataSource

class FetchTechBlogPostReaderTest : UnitTest() {

    @Test
    fun `reader build 시 기본 설정을 구성한다`() {
        val dataSource = mockk<DataSource>()
        val sut = FetchTechBlogPostReader(dataSource)

        val reader = sut.build()

        reader.name shouldBe "fetchTechBlogPostReader"
        reader.pageSize shouldBe 100

        val queryProvider = ReflectionTestUtils.getField(reader, "queryProvider") as MySqlPagingQueryProvider
        val sortKeys = ReflectionTestUtils.getField(queryProvider, "sortKeys") as Map<*, *>
        sortKeys["t.id"] shouldBe Order.ASCENDING
    }
}
