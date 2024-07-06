package com.boardgamegeek.extensions

import android.net.Uri
import com.boardgamegeek.model.Game
import timber.log.Timber
import java.text.DateFormat
import java.util.*

fun String?.replaceHtmlLineFeeds(): String {
    return if (isNullOrBlank()) "" else replace("&#10;", "\n")
}

fun String?.sortName(sortIndex: Int): String {
    if (this == null) return ""
    if (sortIndex <= 1 || sortIndex > length) return this
    val i = sortIndex - 1
    return "${substring(i)}, ${substring(0, i).trim()}"
}

fun String.getImageId(): Int {
    val invalidImageId = 0
    if (isBlank()) return invalidImageId
    "/pic\\d+.".toRegex().find(this)?.let {
        return it.value.findFirstNumber() ?: invalidImageId
    }
    "/avatar_\\d+.".toRegex().find(this)?.let {
        return it.value.findFirstNumber() ?: invalidImageId
    }
    return invalidImageId
}

fun String.findFirstNumber() = "\\d+".toRegex().find(this)?.value?.toIntOrNull()

fun String?.encodeForUrl(): String? = Uri.encode(this, "UTF-8")

fun String?.toMillis(format: DateFormat, defaultMillis: Long = 0L): Long {
    return if (isNullOrBlank()) {
        defaultMillis
    } else {
        try {
            format.parse(this)?.time ?: defaultMillis
        } catch (e: Exception) {
            Timber.w(e, "Unable to parse \"%s\" as \"%s\"", this, format)
            defaultMillis
        }
    }
}

fun String?.asYear(unknownYear: Int = Game.YEAR_UNKNOWN): Int {
    if (this.isNullOrBlank()) return unknownYear
    val l = this.toLong()
    return if (l > Integer.MAX_VALUE) {
        try {
            (l - Long.MAX_VALUE).toInt() - 1
        } catch (e: Exception) {
            unknownYear
        }
    } else {
        this.toIntOrNull() ?: unknownYear
    }
}

fun String?.asCurrency(): String {
    return when (this) {
        null, "USD", "CAD", "AUD" -> "$"
        "EUR" -> "\u20AC"
        "GBP" -> "\u00A3"
        "YEN" -> "\u00A5"
        else -> ""
    }
}

fun String?.toSubtype() = Game.Subtype.entries.find { this == it.code } ?: Game.Subtype.UNKNOWN

@Suppress("NOTHING_TO_INLINE")
inline fun String.andMore() = "${this}+"

@Suppress("NOTHING_TO_INLINE")
inline fun String.andLess() = "<${this}"

fun String?.firstChar(default: String= "-"): String {
    if (isNullOrEmpty()) return default
    return substring(0, 1).uppercase(Locale.getDefault())
}

fun String?.ensureHttpsScheme(): String? {
    return when {
        this == null -> null
        this == "" -> null
        startsWith("//") -> "https:${this}"
        else -> this
    }
}

private const val TRUNCATED_TEXT_SUFFIX = ".."

fun String.truncate(length: Int): String {
    require(length > 0)
    return when {
        this.length <= length -> this
        length > TRUNCATED_TEXT_SUFFIX.length -> this.take(length - TRUNCATED_TEXT_SUFFIX.length) + TRUNCATED_TEXT_SUFFIX
        else -> this.take(length)
    }
}

fun String.toShortLabel() = this.truncate(12)

fun String.toLongLabel() = this.truncate(25)
