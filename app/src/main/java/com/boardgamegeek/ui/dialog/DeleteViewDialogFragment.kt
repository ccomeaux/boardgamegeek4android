package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog.Builder
import com.boardgamegeek.R
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract.CollectionViews

class DeleteViewDialogFragment : DialogFragment() {
    private var listener: OnViewDeletedListener? = null

    interface OnViewDeletedListener {
        fun onViewDeleted(viewId: Long)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? OnViewDeletedListener
        if (listener == null) throw ClassCastException("$context must implement OnViewSavedListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.load(CollectionViews.CONTENT_URI, arrayOf(CollectionViews._ID, CollectionViews.NAME))

        return Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_delete_view)
                .setCursor(cursor, { _, which ->
                    Builder(requireContext())
                            .setTitle(R.string.are_you_sure_title)
                            .setMessage(R.string.are_you_sure_delete_collection_view)
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                if (cursor?.moveToPosition(which) == true) {
                                    val viewId = cursor.getLong(0)
                                    val count = contentResolver.delete(CollectionViews.buildViewUri(viewId), null, null)
                                    if (count == 1) {
                                        listener?.onViewDeleted(viewId)
                                    }
                                }
                            }
                            .setNegativeButton(R.string.no, null)
                            .create()
                            .show()
                }, CollectionViews.NAME)
                .create()
    }

    companion object {
        @JvmStatic
        fun newInstance(): DeleteViewDialogFragment {
            return DeleteViewDialogFragment()
        }
    }
}
