package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

data class PlayPlayerEntity(
        val name: String,
        val username: String,
        val startingPosition: String? = null,
        val color: String? = null,
        val score: String? = null,
        val rating: Double = 0.0,
        val userId: String? = null,
        val isNew: Boolean = false,
        val isWin: Boolean = false,
        val playId: Int = BggContract.INVALID_ID
) {
    private val format = NumberFormat.getInstance()

    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.toLowerCase(Locale.getDefault())}"

    val seat: Int
        get() = startingPosition?.toIntOrNull() ?: SEAT_UNKNOWN

    val numericScore: Double?
        get() = if (score == null)
            null
        else
            try {
                format.parse(score)?.toDouble()
            } catch (ex: ParseException) {
                null
            }

    val description: String = if (username.isBlank()) name else "$name ($username)"

    fun toLongDescription(context: Context): String {
        val sb = StringBuilder()
        if (seat != SEAT_UNKNOWN) sb.append(context.getString(R.string.player_description_starting_position_segment, seat))
        sb.append(name)
        if (username.isNotEmpty()) sb.append(context.getString(R.string.player_description_username_segment, username))
        if (isNew) sb.append(context.getString(R.string.player_description_new_segment))
        if (color?.isBlank() == false) sb.append(context.getString(R.string.player_description_color_segment, color))
        if (score?.isBlank() == false) sb.append(context.getString(R.string.player_description_score_segment, score))
        if (isWin) sb.append(context.getString(R.string.player_description_win_segment))
        return sb.toString()
    }

    companion object {
        const val SEAT_UNKNOWN = -1
    }
}
