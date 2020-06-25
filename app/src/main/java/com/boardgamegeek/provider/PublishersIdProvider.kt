package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggContract.Publishers
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class PublishersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = Publishers.CONTENT_ITEM_TYPE

    override val path = "$PATH_PUBLISHERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val publisherId = Publishers.getPublisherId(uri)
        return SelectionBuilder()
                .table(Tables.PUBLISHERS)
                .whereEquals(Publishers.PUBLISHER_ID, publisherId)
    }
}
