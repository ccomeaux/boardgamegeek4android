package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.PlayerColorsColumns.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

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