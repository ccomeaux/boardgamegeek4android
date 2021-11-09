@file:JvmName("StringUtils")

package com.boardgamegeek.extensions

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.io.BggService
import timber.log.Timber
import java.text.DateFormat
import java.util.*

fun String?.replaceHtmlLineFeeds(): String {
    return if (this == null || isBlank()) "" else replace("&#10;", "\n")
}

fun String?.sortName(sortIndex: Int): String {
    if (this == null) return ""
    if (sortIndex <= 1 || sortIndex > length) return this
    val i = sortIndex - 1
    return "${substring(i)}, ${substring(0, i).trim()}"
}

/**
 * Describes the rank with either the subtype or the family name.
 */
fun String.asRankDescription(context: Context, type: String = BggService.RANK_TYPE_SUBTYPE): CharSequence {
    when (type) {
        BggService.RANK_TYPE_SUBTYPE -> {
            @StringRes val resId = when (this) {
                BggService.THING_SUBTYPE_BOARDGAME -> R.string.title_board_game
                BggService.THING_SUBTYPE_BOARDGAME_EXPANSION -> R.string.title_expansion
                BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY -> R.string.title_accessory
                else -> return this
            }
            return context.getText(resId)
        }
        BggService.RANK_TYPE_FAMILY -> {
            @StringRes val resId = when (this) {
                BggService.RANK_FAMILY_NAME_ABSTRACT_GAMES -> R.string.title_abstract
                BggService.RANK_FAMILY_NAME_CHILDRENS_GAMES -> R.string.title_childrens
                BggService.RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES -> R.string.title_customizable
                BggService.RANK_FAMILY_NAME_FAMILY_GAMES -> R.string.title_family
                BggService.RANK_FAMILY_NAME_PARTY_GAMES -> R.string.title_party
                BggService.RANK_FAMILY_NAME_STRATEGY_GAMES -> R.string.title_strategy
                BggService.RANK_FAMILY_NAME_THEMATIC_GAMES -> R.string.title_thematic
                BggService.RANK_FAMILY_NAME_WAR_GAMES -> R.string.title_war
                else -> return this
            }
            return context.getText(resId)
        }
        else -> return context.getText(R.string.title_game)
    }
}

fun String?.toMillis(format: DateFormat, defaultMillis: Long = 0L): Long {
    return if (isNullOrBlank()) {
        defaultMillis
    } else {
        try {
            format.parse(this)?.time ?: defaultMillis
        } catch (e: Exception) {
            Timber.w(e, "Unable to parse \"%s\"", this)
            defaultMillis
        }
    }
}

fun String?.asYear(unknownYear: Int = GameEntity.YEAR_UNKNOWN): Int {
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

@Suppress("NOTHING_TO_INLINE")
inline fun String.andMore() = "${this}+"

@Suppress("NOTHING_TO_INLINE")
inline fun String.andLess() = "<${this}"

fun String?.firstChar(): String {
    if (this == null || isEmpty()) return "-"
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
