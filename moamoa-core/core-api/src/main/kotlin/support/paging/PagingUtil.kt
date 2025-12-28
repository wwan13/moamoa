package support.paging

import kotlin.math.ceil

fun calculateTotalPage(totalCount: Long, size: Long) =
    if (totalCount == 0L) 0L else ceil(totalCount.toDouble() / size.toDouble()).toLong()