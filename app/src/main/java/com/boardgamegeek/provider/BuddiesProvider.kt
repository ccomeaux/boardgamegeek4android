package com.boardgamegeek.provider

import android.content.ContentValues
import android.net.Uri
import com.boardgamegeek.provider.BggContract.Buddies
import com.boardgamegeek.provider.BggContract.PATH_BUDDIES
import com.boardgamegeek.provider.BggDatabase.Tables

class BuddiesProvider : BasicProvider() {
    override fun getType(uri: Uri) = Buddies.CONTENT_TYPE

    override val path: String = PATH_BUDDIES

    override val table = Tables.BUDDIES

    override val defaultSortOrder = Buddies.DEFAULT_SORT

    override fun insertedUri(values: ContentValues?, rowId: Long): Uri? {
        val buddyName = values?.getAsString(Buddies.BUDDY_NAME)
        return if (buddyName.isNullOrBlank()) null else Buddies.buildBuddyUri(buddyName)
    }
}