package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditCollectionWantPartsDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.want_parts_list

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updateWantParts(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionWantPartsDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionWantPartsDialogFragment.TAG)
        }
    }
}
