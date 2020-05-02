package com.boardgamegeek.provider

import android.content.ContentValues
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.PATH_BUDDIES
import com.boardgamegeek.provider.BggDatabase.Tables

class BuddiesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Buddies.CONTENT_TYPE

    override fun getPath(): String = PATH_BUDDIES

    override val table = Tables.BUDDIES

    override fun getDefaultSortOrder() = Buddies.DEFAULT_SORT

    override fun insertedUri(values: ContentValues, rowId: Long): Uri {
        return Buddies.buildBuddyUri(values.getAsString(Buddies.BUDDY_NAME))
    }
}