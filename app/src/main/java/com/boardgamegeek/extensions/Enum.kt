package com.boardgamegeek.extensions

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.io.BggService

fun BggService.ThingSubtype?.getDescription(context: Context): String {
    return context.getString(
        when (this) {
            BggService.ThingSubtype.BOARDGAME -> R.string.games
            BggService.ThingSubtype.BOARDGAME_EXPANSION -> R.string.expansions
            BggService.ThingSubtype.BOARDGAME_ACCESSORY -> R.string.accessories
            else -> R.string.items
        }
    )
}

