package server.batch.common.time

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

internal fun NamedParameterJdbcTemplate.dbNow() =
    queryForObject("SELECT NOW()", emptyMap<String, Any>(), LocalDateTime::class.java)!!
