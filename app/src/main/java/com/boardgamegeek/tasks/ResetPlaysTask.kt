package com.boardgamegeek.tasks

import android.content.Context
import androidx.core.content.contentValuesOf
import com.boardgamegeek.R
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.service.SyncService
import timber.log.Timber

/**
 * Clears the plays sync timestamps and requests a full plays sync be performed.
 */
class ResetPlaysTask(context: Context?) : ToastingAsyncTask(context) {
    override val successMessageResource = R.string.pref_sync_reset_success

    override val failureMessageResource = R.string.pref_sync_reset_failure

    override fun doInBackground(vararg params: Void?): Boolean {
        context?.applicationContext?.let {
            SyncPrefs.getPrefs(it).clearPlaysTimestamps()
            val values = contentValuesOf(Plays.SYNC_HASH_CODE to 0)
            val count = it.contentResolver.update(Plays.CONTENT_URI, values, null, null)
            Timber.i("Cleared the hashcode from %,d plays.", count)
            SyncService.sync(it, SyncService.FLAG_SYNC_PLAYS)
            return true
        } ?: return false
    }
}