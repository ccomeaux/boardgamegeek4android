package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.PATH_BUDDIES
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class BuddiesIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Buddies.CONTENT_ITEM_TYPE

    override val path: String = "$PATH_BUDDIES/*"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val buddyName = Buddies.getBuddyName(uri)
        return SelectionBuilder()
                .table(Tables.BUDDIES)
                .whereEquals(Buddies.BUDDY_NAME, buddyName)
    }
}
