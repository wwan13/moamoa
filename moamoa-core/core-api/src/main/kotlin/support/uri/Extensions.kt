package support.uri

import java.net.URI

fun String.toUri(): URI = URI.create(this)
