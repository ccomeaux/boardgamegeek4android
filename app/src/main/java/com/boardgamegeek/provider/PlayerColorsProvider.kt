package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_PLAYER_COLORS
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.provider.BggDatabase.Tables

class PlayerColorsProvider : BasicProvider() {
    override fun getType(uri: Uri) = PlayerColors.CONTENT_TYPE

    override val path = PATH_PLAYER_COLORS

    override val table = Tables.PLAYER_COLORS

    override val defaultSortOrder = PlayerColors.DEFAULT_SORT
}