package com.boardgamegeek.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.boardgamegeek.extensions.get
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_NEWEST_DATE
import com.boardgamegeek.pref.SyncPrefs.Companion.TIMESTAMP_PLAYS_OLDEST_DATE

class SyncTimestampsDialogFragment : PreferenceDialogFragmentCompat() {
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(requireContext()) }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val collectionFull = view.findViewById<TextView>(R.id.sync_timestamp_collection_full)
        val collectionPartial = view.findViewById<TextView>(R.id.sync_timestamp_collection_partial)
        val buddies = view.findViewById<TextView>(R.id.sync_timestamp_buddy)
        val playsView = view.findViewById<TextView>(R.id.sync_timestamp_plays)

        collectionFull.text = syncPrefs.getLastCompleteCollectionTimestamp()
            .formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
        collectionPartial.text = syncPrefs.getPartialCollectionSyncLastCompletedAt()
            .formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
        buddies.text = syncPrefs.getBuddiesTimestamp()
            .formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR)

        val oldestDate = syncPrefs[TIMESTAMP_PLAYS_OLDEST_DATE] ?: Long.MAX_VALUE
        val newestDate = syncPrefs[TIMESTAMP_PLAYS_NEWEST_DATE] ?: 0L
        playsView.text = when {
            oldestDate == Long.MAX_VALUE && newestDate <= 0L -> getString(R.string.plays_sync_status_none)
            oldestDate <= 0L -> String.format(getString(R.string.plays_sync_status_new), newestDate.asDate())
            newestDate <= 0L -> String.format(getString(R.string.plays_sync_status_old), oldestDate.asDate())
            else -> String.format(
                getString(R.string.plays_sync_status_range),
                oldestDate.asDate(),
                newestDate.asDate()
            )
        }
    }

    private fun Long.asDate() = this.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)

    override fun onDialogClosed(positiveResult: Boolean) {
        // do nothing
    }

    companion object {
        fun newInstance(key: String): SyncTimestampsDialogFragment {
            return SyncTimestampsDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_KEY, key) }
            }
        }
    }
}
