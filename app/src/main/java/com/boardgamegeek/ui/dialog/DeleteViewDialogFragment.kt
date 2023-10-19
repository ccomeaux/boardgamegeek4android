package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteViewDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewViewModel::class.java]

        val views = viewModel.views.value.orEmpty().filter { it.id != CollectionViewPrefs.DEFAULT_DEFAULT_ID }
        val viewNames = views.map { it.name }.toTypedArray()

        return requireContext().createThemedBuilder()
            .setTitle(R.string.title_delete_view)
            .setItems(viewNames) { _, which ->
                viewNames.getOrNull(which)?.let { viewName ->
                    Builder(requireContext())
                        .setTitle(R.string.are_you_sure_title)
                        .setMessage(getString(R.string.are_you_sure_delete_collection_view, viewName))
                        .setCancelable(true)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            views.find { it.name == viewName }?.id?.let { viewId ->
                                viewModel.deleteView(viewId, viewName)
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
