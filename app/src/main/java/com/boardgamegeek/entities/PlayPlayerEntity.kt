package com.boardgamegeek.entities

import android.os.Parcelable
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

@Parcelize
data class PlayPlayerEntity(
    val name: String = "",
    val username: String = "",
    val startingPosition: String = "",
    val color: String = "",
    val score: String = "",
    val rating: Double = DEFAULT_RATING,
    val userId: Int? = INVALID_ID,
    val isNew: Boolean = false,
    val isWin: Boolean = false,
    val playId: Int = INVALID_ID,
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

    val fullDescription: String
        get() {
            var description = ""
            if (name.isEmpty()) {
                if (username.isEmpty()) {
                    if (color.isNotBlank()) {
                        description = color
                    }
                } else {
                    description = username
                }
            } else {
                description = name
                if (username.isNotEmpty()) {
                    description += " ($username)"
                }
            }
            return description
        }

    companion object {
        const val SEAT_UNKNOWN = -1
        const val DEFAULT_RATING = 0.0
    }
}
