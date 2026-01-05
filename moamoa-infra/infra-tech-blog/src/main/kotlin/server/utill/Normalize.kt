package server.utill

import java.text.Normalizer


private val INVISIBLE_CHARS = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\u00A0]")
private val CONTROL_CHARS = Regex("\\p{C}+")
private val MULTI_SPACE = Regex("\\s+")

fun String.normalizeTagTitle(): String =
    Normalizer.normalize(this, Normalizer.Form.NFKC)
        .replace(INVISIBLE_CHARS, "")
        .replace(CONTROL_CHARS, "")
        .replace(MULTI_SPACE, " ")
        .trim()