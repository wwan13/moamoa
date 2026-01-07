package server.member.reader

import org.springframework.batch.item.database.JdbcCursorItemReader
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.stereotype.Component
import server.member.dto.MemberData
import javax.sql.DataSource

@Component
class GenerateAlarmContentReader(
    private val dataSource: DataSource,
) {

    fun build() = JdbcCursorItemReader<MemberData>().apply {
        setName("generateAlarmContentReader")
        setDataSource(this@GenerateAlarmContentReader.dataSource)
        setSql(
            """
                SELECT m.id AS member_id, m.email AS email
                FROM member m
                """.trimIndent()
        )
        setRowMapper(DataClassRowMapper(MemberData::class.java))
    }
}