package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PLAYERS
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.provider.BggDatabase.Tables

class PlayersNameColorsOrderProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayerColors.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYERS/*/$PATH_COLORS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val playerName = PlayerColors.getPlayerName(uri)
        val sortOrder = PlayerColors.getSortOrder(uri)
        return SelectionBuilder().table(Tables.PLAYER_COLORS)
            .where("${PlayerColors.Columns.PLAYER_TYPE}=?", PlayerColors.TYPE_PLAYER.toString())
            .where("${PlayerColors.Columns.PLAYER_NAME}=?", playerName)
            .where("${PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER}=?", sortOrder.toString())
    }
}
