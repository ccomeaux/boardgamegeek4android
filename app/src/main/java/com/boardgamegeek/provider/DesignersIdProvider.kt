package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Designers
import com.boardgamegeek.provider.BggContract.PATH_DESIGNERS
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class DesignersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Designers.CONTENT_ITEM_TYPE

    override val path = "$PATH_DESIGNERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val designerId = Designers.getDesignerId(uri)
        return SelectionBuilder().table(Tables.DESIGNERS).whereEquals(Designers.DESIGNER_ID, designerId)
    }
}
