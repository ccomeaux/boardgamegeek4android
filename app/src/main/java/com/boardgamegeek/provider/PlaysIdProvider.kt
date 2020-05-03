package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_PLAYS
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PlaysIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Plays.CONTENT_ITEM_TYPE

    override val path = "$PATH_PLAYS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val internalId = Plays.getInternalId(uri)
        return SelectionBuilder()
                .table(Tables.PLAYS)
                .whereEquals(Plays._ID, internalId.toString())
    }
}