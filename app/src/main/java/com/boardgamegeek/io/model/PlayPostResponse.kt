@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.io.model

import com.boardgamegeek.provider.BggContract
import com.google.gson.annotations.SerializedName

class PlayPostResponse {
    @SerializedName("playid")
    val playId = BggContract.INVALID_ID
    @SerializedName("numplays")
    val numberOfPlays = 0
    val html: String? = null // Plays: <a href="/plays/thing/${gameId}?userid=${userId}">${numberOfPlays}</a>
    val error: String? = null
    val success: Boolean = false

    fun hasAuthError(): Boolean {
        return "You must login to save plays".equals(error, ignoreCase = true) ||
                "You can't delete this play".equals(error, ignoreCase = true) ||
                "You are not permitted to edit this play.".equals(error, ignoreCase = true)
    }

    fun hasInvalidIdError(): Boolean {
        return "Play does not exist.".equals(error, ignoreCase = true) ||
                "Invalid item. Play not saved.".equals(error, ignoreCase = true)
    }
}
