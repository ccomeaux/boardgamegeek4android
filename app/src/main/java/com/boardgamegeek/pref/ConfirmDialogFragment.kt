package com.boardgamegeek.pref

import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.tasks.*

class ConfirmDialogFragment : PreferenceDialogFragmentCompat() {
    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val key = arguments?.getString(ARG_KEY)
            val task: ToastingAsyncTask? = when (key) {
                "clear" -> ClearDatabaseTask(context)
                "collection" -> ResetCollectionTask(context)
                "plays" -> ResetPlaysTask(context)
                "buddies" -> ResetBuddiesTask(context)
                else -> null
            }
            task?.executeAsyncTask()
        }
    }

    companion object {
        fun newInstance(key: String): ConfirmDialogFragment {
            return ConfirmDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_KEY, key) }
            }
        }
    }
}
