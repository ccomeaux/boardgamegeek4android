package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditCollectionConditionDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.trade_condition

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updateCondition(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionConditionDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionConditionDialogFragment.TAG)
        }
    }
}
