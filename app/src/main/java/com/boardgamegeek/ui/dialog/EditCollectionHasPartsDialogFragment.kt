package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel

class EditCollectionHasPartsDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.has_parts_list

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updateHasParts(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionHasPartsDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionHasPartsDialogFragment.TAG)
        }
    }
}
