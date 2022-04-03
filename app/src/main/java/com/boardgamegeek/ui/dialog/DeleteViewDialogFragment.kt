package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class DeleteViewDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]
        val firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        val views = viewModel.views.value.orEmpty()
        val viewNames = views.map { it.name }.toTypedArray()

        return Builder(requireContext(), R.style.Theme_bgglight_Dialog_Alert)
            .setTitle(R.string.title_delete_view)
            .setItems(viewNames) { _, which ->
                viewNames.getOrNull(which)?.let { viewName ->
                    val toast = Toast.makeText(requireContext(), getString(R.string.msg_collection_view_deleted, viewName), Toast.LENGTH_SHORT)
                    Builder(requireContext())
                        .setTitle(R.string.are_you_sure_title)
                        .setMessage(getString(R.string.are_you_sure_delete_collection_view, viewName))
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            val viewId = views.find { it.name == viewName }?.id ?: BggContract.INVALID_ID.toLong()
                            if (viewId > 0L) {
                                viewModel.deleteView(viewId)
                                toast.show()
                                firebaseAnalytics.logEvent("DataManipulation") {
                                    param(FirebaseAnalytics.Param.CONTENT_TYPE, "CollectionView")
                                    param("Action", "Delete")
                                    param("ViewName", viewName)
                                }
                            }
                        }
                        .setNegativeButton(R.string.no, null)
                        .create()
                        .show()

                }
            }
            .create()
    }

    companion object {
        fun newInstance() = DeleteViewDialogFragment()
    }
}
