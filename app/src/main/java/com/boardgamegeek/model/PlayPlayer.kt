package com.boardgamegeek.model

import android.content.Context
import android.os.Parcelable
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale
import java.util.UUID

@Parcelize
data class PlayPlayer(
    val name: String = "",
    val username: String = "",
    val startingPosition: String = "",
    val color: String = "",
    val score: String = "",
    val rating: Double = DEFAULT_RATING,
    val userId: Int? = INVALID_ID,
    val isNew: Boolean = false,
    val isWin: Boolean = false,
    val playInternalId: Long = INVALID_ID.toLong(),
    val uiId: Long = UUID.randomUUID().toString().hashCode().toLong(),
    val internalId: Long = INVALID_ID.toLong(),
) : Parcelable {
    @IgnoredOnParcel
    private val format = NumberFormat.getInstance()

    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.lowercase(Locale.getDefault())}"

    val seat: Int
        get() = startingPosition.toIntOrNull() ?: SEAT_UNKNOWN

    @IgnoredOnParcel
    val numericScore: Double? // This attempts to handle localization
        get() = try {
            format.parse(score)?.toDouble()
        } catch (ex: ParseException) {
            null
        }

    @IgnoredOnParcel
    val description: String = if (username.isBlank()) name else "$name ($username)"

    fun fullDescription(context: Context): String = if (name.isEmpty()) {
        username.ifEmpty {
            when {
                color.isNotBlank() -> color
                seat != SEAT_UNKNOWN -> context.getString(R.string.generic_player, seat)
                else -> context.getString(R.string.title_player)
            }
        }
    } else {
        if (username.isEmpty()) name else "$name ($username)"
    }

    companion object {
        const val SEAT_UNKNOWN = -1
        const val DEFAULT_RATING = Game.UNRATED
    }
}
