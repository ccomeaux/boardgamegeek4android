package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog.Builder
import com.boardgamegeek.R
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract.CollectionViews
import org.jetbrains.anko.support.v4.ctx

class DeleteViewDialogFragment : DialogFragment() {
    private var listener: OnViewDeletedListener? = null

    interface OnViewDeletedListener {
        fun onDeleteRequested(viewId: Long)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentResolver = ctx.contentResolver
        val cursor = contentResolver.load(CollectionViews.CONTENT_URI, arrayOf(CollectionViews._ID, CollectionViews.NAME))

        return Builder(ctx, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(R.string.title_delete_view)
                .setCursor(cursor, { _, which ->
                    Builder(ctx)
                            .setTitle(R.string.are_you_sure_title)
                            .setMessage(R.string.are_you_sure_delete_collection_view)
                            .setCancelable(true)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                if (cursor?.moveToPosition(which) == true) {
                                    val viewId = cursor.getLong(0)
                                    val count = contentResolver.delete(CollectionViews.buildViewUri(viewId), null, null)
                                    if (count == 1) {
                                        listener?.onDeleteRequested(viewId)
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
        fun newInstance(listener: OnViewDeletedListener): DeleteViewDialogFragment {
            val dialogFragment = DeleteViewDialogFragment()
            dialogFragment.listener = listener
            return dialogFragment
        }
    }
}
