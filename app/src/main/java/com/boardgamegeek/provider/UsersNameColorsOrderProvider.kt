package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLORS
import com.boardgamegeek.provider.BggContract.Companion.PATH_USERS
import com.boardgamegeek.provider.BggContract.PlayerColors
import com.boardgamegeek.provider.BggContract.PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER
import com.boardgamegeek.provider.BggContract.PlayerColors.Columns.PLAYER_NAME
import com.boardgamegeek.provider.BggContract.PlayerColors.Columns.PLAYER_TYPE
import com.boardgamegeek.provider.BggDatabase.Tables

class UsersNameColorsOrderProvider : BaseProvider() {
    override fun getType(uri: Uri) = PlayerColors.CONTENT_ITEM_TYPE

    override val path = "$PATH_USERS/*/$PATH_COLORS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder().table(Tables.PLAYER_COLORS)
            .where("$PLAYER_TYPE=?", PlayerColors.TYPE_USER.toString())
            .where("$PLAYER_NAME=?", PlayerColors.getUsername(uri))
            .where("$PLAYER_COLOR_SORT_ORDER=?", PlayerColors.getSortOrder(uri).toString())
    }
}
