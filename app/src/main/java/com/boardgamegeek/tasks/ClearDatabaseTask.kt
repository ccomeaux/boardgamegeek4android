package com.boardgamegeek.tasks

import android.content.Context
import android.net.Uri
import com.boardgamegeek.R
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearBuddyListTimestamps
import com.boardgamegeek.pref.clearCollection
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.*
import timber.log.Timber

/**
 * Deletes all data in the local database.
 */
class ClearDatabaseTask(context: Context?) : ToastingAsyncTask(context) {
    override val successMessageResource = R.string.pref_sync_clear_success

    override val failureMessageResource = R.string.pref_sync_clear_failure

    override fun doInBackground(vararg params: Void?): Boolean {
        context?.applicationContext?.let {
            val syncPrefs = SyncPrefs.getPrefs(it)
            syncPrefs.clearCollection()
            syncPrefs.clearBuddyListTimestamps()
            syncPrefs.clearPlaysTimestamps()

            var count = 0
            count += delete(Games.CONTENT_URI)
            count += delete(Artists.CONTENT_URI)
            count += delete(Designers.CONTENT_URI)
            count += delete(Publishers.CONTENT_URI)
            count += delete(Categories.CONTENT_URI)
            count += delete(Mechanics.CONTENT_URI)
            count += delete(Buddies.CONTENT_URI)
            count += delete(Plays.CONTENT_URI)
            count += delete(CollectionViews.CONTENT_URI)
            Timber.i("Removed %d records", count)

            count = 0
            count += it.contentResolver.delete(Thumbnails.CONTENT_URI, null, null)
            count += it.contentResolver.delete(Avatars.CONTENT_URI, null, null)
            Timber.i("Removed %d files", count)

            return true
        } ?: return false
    }

    private fun delete(uri: Uri): Int {
        val count = context?.applicationContext?.contentResolver?.delete(uri, null, null) ?: 0
        Timber.i("Removed %1\$d %2\$s", count, uri.lastPathSegment)
        return count
    }
}
