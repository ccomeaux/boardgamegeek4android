package com.boardgamegeek.service

import android.content.ContentValues
import com.boardgamegeek.provider.BggContract
import okhttp3.FormBody
import okhttp3.OkHttpClient
import timber.log.Timber

class CollectionStatusUploadTask(client: OkHttpClient) : CollectionUploadTask(client) {
    //	<div class='owned'>Owned</div>
    //	<div class='wishlist'>Wishlist(3)
    //	<br>&nbsp;(Like&nbsp;to&nbsp;have)
    //	</div>
    override val timestampColumn = BggContract.Collection.STATUS_DIRTY_TIMESTAMP

    override val isDirty = collectionItem.statusTimestamp > 0

    override fun createForm(): FormBody {
        @Suppress("SpellCheckingInspection")
        return createFormBuilder()
                .add("fieldname", "status")
                .add("own", if (collectionItem.owned) "1" else "0")
                .add("prevowned", if (collectionItem.previouslyOwned) "1" else "0")
                .add("fortrade", if (collectionItem.forTrade) "1" else "0")
                .add("want", if (collectionItem.wantInTrade) "1" else "0")
                .add("wanttobuy", if (collectionItem.wantToBuy) "1" else "0")
                .add("wanttoplay", if (collectionItem.wantToPlay) "1" else "0")
                .add("preordered", if (collectionItem.preordered) "1" else "0")
                .add("wishlist", if (collectionItem.wishlist) "1" else "0")
                .add("wishlistpriority", collectionItem.wishlistPriority.toString())
                .build()
    }

    override fun saveContent(content: String) {
        Timber.d(content)
    }

    override fun appendContentValues(contentValues: ContentValues) {
        contentValues.put(BggContract.Collection.STATUS_DIRTY_TIMESTAMP, 0)
    }
}