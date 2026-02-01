package server.infra.db.databaseclient

import io.r2dbc.spi.Row

inline fun <reified T : Any> Row.getOrDefault(column: String, default: T): T {
    return try {
        this.get(column, T::class.java) ?: default
    } catch (_: IllegalArgumentException) {
        default
    } catch (_: Exception) { // 드라이버별 예외 차이 방어
        default
    }
}

fun Row.getInt01(column: String): Boolean {
    val v = getOrDefault(column, 0)
    return v == 1
}