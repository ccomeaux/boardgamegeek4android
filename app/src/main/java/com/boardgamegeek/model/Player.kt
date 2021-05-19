package com.boardgamegeek.model

import android.os.Parcelable
import com.boardgamegeek.extensions.asScore
import kotlinx.android.parcel.Parcelize
import java.text.DecimalFormat
import java.util.*

@Parcelize
data class Player @JvmOverloads constructor(
        @JvmField
        var name: String = "",
        @JvmField
        var username: String = "",
        @JvmField
        var color: String = "",
        @JvmField
        var startingPosition: String = "",
        @JvmField
        var score: String = "",

        @JvmField
        var userId: Int = 0,

        @JvmField
        var rating: Double = 0.0,

        @JvmField
        var isNew: Boolean = false,

        @JvmField
        var isWin: Boolean = false,
) : Parcelable {
    val id: String
        get() = createId(username, name)

    var seat: Int
        get() = startingPosition.toIntOrNull() ?: SEAT_UNKNOWN
        set(value) {
            startingPosition = value.toString()
        }

    val description: String = if (username.isBlank()) name else "$name ($username)"

    val fullDescription: String
        get() {
            var description = ""
            if (name.isEmpty()) {
                if (username.isEmpty()) {
                    if (color.isNotEmpty()) {
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
        const val DEFAULT_RATING = 0.0
        const val SEAT_UNKNOWN = -1

        fun createId(username: String, name: String) =
                if (username.isBlank()) "P|$name" else "U|${username.toLowerCase(Locale.getDefault())}"
    }
}
