package com.boardgamegeek.tasks

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.os.AsyncTask
import com.boardgamegeek.events.CollectionItemUpdatedEvent
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.ResolverUtils
import org.greenrobot.eventbus.EventBus

abstract class UpdateCollectionItemTask(context: Context?, protected val gameId: Int, protected val collectionId: Int, protected var internalId: Long) : AsyncTask<Void, Void, Boolean>() {
    @SuppressLint("StaticFieldLeak")
    private val context: Context? = context?.applicationContext

    override fun doInBackground(vararg params: Void): Boolean? {
        if (context == null) return null
        val resolver = context.contentResolver
        if (internalId == 0L) {
            internalId = getCollectionItemInternalId(resolver, collectionId, gameId)
        }
        return if (internalId != BggContract.INVALID_ID.toLong()) {
            updateResolver(resolver, internalId)
        } else false
    }

    override fun onPostExecute(result: Boolean?) {
        if (result != null && result)
            EventBus.getDefault().post(CollectionItemUpdatedEvent(internalId))
    }

    private fun getCollectionItemInternalId(resolver: ContentResolver, collectionId: Int, gameId: Int): Long {
        return if (collectionId == BggContract.INVALID_ID) {
            ResolverUtils.queryLong(resolver,
                    Collection.CONTENT_URI,
                    Collection._ID,
                    BggContract.INVALID_ID,
                    "collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID + " IS NULL",
                    arrayOf(gameId.toString()))
        } else {
            ResolverUtils.queryLong(resolver,
                    Collection.CONTENT_URI,
                    Collection._ID,
                    BggContract.INVALID_ID,
                    Collection.COLLECTION_ID + "=?",
                    arrayOf(collectionId.toString()))
        }
    }

    protected abstract fun updateResolver(resolver: ContentResolver, internalId: Long): Boolean
}
