package com.boardgamegeek.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime

class SyncTimestampsDialogFragment : PreferenceDialogFragmentCompat() {
    private val syncPrefs: SharedPreferences by lazy { SyncPrefs.getPrefs(requireContext()) }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val collectionFullCurrentLayout = view.findViewById<ViewGroup>(R.id.sync_timestamp_collection_full_current_layout)
        val collectionFullCurrent = view.findViewById<TextView>(R.id.sync_timestamp_collection_full_current)
        val collectionFull = view.findViewById<TextView>(R.id.sync_timestamp_collection_full)
        val collectionPartial = view.findViewById<TextView>(R.id.sync_timestamp_collection_partial)

        val currentCollectionSyncTimestamp = syncPrefs.getCurrentCollectionSyncTimestamp()
        if (currentCollectionSyncTimestamp > 0L) {
            collectionFullCurrent.text = currentCollectionSyncTimestamp.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
        }
        collectionFullCurrentLayout.isVisible = (currentCollectionSyncTimestamp > 0L)

        collectionFull.text = syncPrefs.getLastCompleteCollectionTimestamp()
            .formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
        collectionPartial.text = syncPrefs.getPartialCollectionSyncLastCompletedAt()
            .formatDateTime(context, flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
    }

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
