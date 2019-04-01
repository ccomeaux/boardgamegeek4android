package com.boardgamegeek.pref

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.R

class SyncTimestampsDialogFragment : PreferenceDialogFragmentCompat() {
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val collectionFull = view.findViewById<TextView>(R.id.sync_timestamp_collection_full)
        val collectionPartial = view.findViewById<TextView>(R.id.sync_timestamp_collection_partial)
        val buddies = view.findViewById<TextView>(R.id.sync_timestamp_buddy)
        val playsView = view.findViewById<TextView>(R.id.sync_timestamp_plays)

        setDateTime(collectionFull, SyncPrefs.getLastCompleteCollectionTimestamp(requireContext()), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        setDateTime(collectionPartial, SyncPrefs.getLastPartialCollectionTimestamp(requireContext()), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

        setDateTime(buddies, SyncPrefs.getBuddiesTimestamp(requireContext()), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)

        val oldestDate = SyncPrefs.getPlaysOldestTimestamp(requireContext())
        val newestDate = SyncPrefs.getPlaysNewestTimestamp(requireContext())
        if (oldestDate == java.lang.Long.MAX_VALUE && (newestDate == null || newestDate <= 0L)) {
            playsView.setText(R.string.plays_sync_status_none)
        } else {
            playsView.text = if (newestDate == null || newestDate <= 0L) {
                String.format(requireContext().getString(R.string.plays_sync_status_old),
                        DateUtils.formatDateTime(requireContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE))
            } else if (oldestDate <= 0L) {
                String.format(requireContext().getString(R.string.plays_sync_status_new),
                        DateUtils.formatDateTime(requireContext(), newestDate, DateUtils.FORMAT_SHOW_DATE))
            } else {
                String.format(requireContext().getString(R.string.plays_sync_status_range),
                        DateUtils.formatDateTime(requireContext(), oldestDate, DateUtils.FORMAT_SHOW_DATE),
                        DateUtils.formatDateTime(requireContext(), newestDate, DateUtils.FORMAT_SHOW_DATE))
            }
        }
    }

    private fun setDateTime(view: TextView, timeStamp: Long, flags: Int) {
        view.text = if (timeStamp == 0L) {
            requireContext().getString(R.string.never)
        } else {
            DateUtils.formatDateTime(context, timeStamp, flags)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // do nothing
    }

    companion object {
        fun newInstance(key: String): SyncTimestampsDialogFragment {
            return SyncTimestampsDialogFragment().apply {
                arguments = Bundle().apply { putString(PreferenceDialogFragmentCompat.ARG_KEY, key) }
            }
        }
    }
}
