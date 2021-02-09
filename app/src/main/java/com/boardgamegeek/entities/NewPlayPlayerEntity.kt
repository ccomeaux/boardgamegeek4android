package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.extensions.asScore
import java.text.DecimalFormat
import java.util.*

data class NewPlayPlayerEntity(
        val name: String,
        val username: String,
        private val rawAvatarUrl: String = "") {
    constructor(player: PlayerEntity) : this(player.name, player.username, player.rawAvatarUrl)

    val id: String
        get() = if (username.isBlank()) "P|$name" else "U|${username.toLowerCase(Locale.getDefault())}"

    val avatarUrl: String = rawAvatarUrl
        get() = if (field == "N/A") "" else field

    val description: String = if (username.isBlank()) name else "$name ($username)"

    var isNew: Boolean = false

    var isWin: Boolean = false

    var score: String = ""

    fun getScoreDescription(context: Context?): String {
        return score.toDoubleOrNull()?.asScore(context, format = DecimalFormat("#,##0.###")) ?: score
    }

    var color: String = ""

    var sortOrder: String = ""

    val seat: Int?
        get() = sortOrder.toIntOrNull()

    var favoriteColors = emptyList<String>()
}
