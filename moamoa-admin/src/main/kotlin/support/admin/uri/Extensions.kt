package support.admin.uri

import java.net.URI

internal fun String.toUri(): URI = URI.create(this)
