package com.boardgamegeek.tasks

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.pref.SyncPrefs.Companion.getPrefs
import com.boardgamegeek.pref.clearCollection
import com.boardgamegeek.service.SyncService

/**
 * Clears the collection sync timestamps and requests a full collection sync be performed.
 */
class ResetCollectionTask(context: Context?) : ToastingAsyncTask(context) {
    override val successMessageResource = R.string.pref_sync_reset_success

    override val failureMessageResource = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void?): Boolean {
        return context?.applicationContext?.let {
            getPrefs(it).clearCollection()
            SyncService.sync(it, SyncService.FLAG_SYNC_COLLECTION)
            true
        } ?: false
    }
}
