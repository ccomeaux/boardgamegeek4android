package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.Companion.PATH_BUDDIES
import com.boardgamegeek.provider.BggDatabase.Tables

class BuddiesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Buddies.CONTENT_ITEM_TYPE

    override val path: String = "$PATH_BUDDIES/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val buddyName = Buddies.getBuddyName(uri)
        return SelectionBuilder()
            .table(Tables.BUDDIES)
            .whereEquals(Buddies.Columns.BUDDY_NAME, buddyName)
    }
}
