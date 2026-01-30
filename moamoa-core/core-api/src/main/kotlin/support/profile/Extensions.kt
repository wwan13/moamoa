package support.profile

import org.springframework.core.env.Environment

fun Environment.isProd() =
    activeProfiles.any { it.equals("prod", ignoreCase = true) }