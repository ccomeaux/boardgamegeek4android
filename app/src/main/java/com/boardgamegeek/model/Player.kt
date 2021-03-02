package com.boardgamegeek.model

import android.os.Parcelable
import com.boardgamegeek.extensions.asScore
import kotlinx.android.parcel.Parcelize
import java.text.DecimalFormat

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
    var seat: Int
        get() = startingPosition.toIntOrNull() ?: SEAT_UNKNOWN
        set(value) {
            startingPosition = value.toString()
        }

    val ratingDescription: String
        get() = if (rating in 1.0..10.0) {
            rating.asScore(format = DecimalFormat("0.#")) // TODO better extension method
        } else ""

    val scoreDescription: String
        get() = score.toDoubleOrNull()?.asScore() ?: score

    val description: String
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
    }
}