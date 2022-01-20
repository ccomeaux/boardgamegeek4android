package com.boardgamegeek.tasks

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.SyncService
import timber.log.Timber

/**
 * Clears the GeekBuddies sync timestamps and requests a full GeekBuddies sync be performed.
 */
class ResetBuddiesTask(context: Context?) : ToastingAsyncTask(context) {
    override val successMessageResource = R.string.pref_sync_reset_success

    override val failureMessageResource = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void?): Boolean {
        return context?.applicationContext?.let {
            SyncPrefs.getPrefs(it).clearBuddyListTimestamps()
            val count = it.contentResolver.delete(BggContract.Buddies.CONTENT_URI, null, null)
            //TODO remove buddy colors
            Timber.i("Removed %d GeekBuddies", count)
            SyncService.sync(it, SyncService.FLAG_SYNC_BUDDIES)
            true
        } ?: false
    }
}
