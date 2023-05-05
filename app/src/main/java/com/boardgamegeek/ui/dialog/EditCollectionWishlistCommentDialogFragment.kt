package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentManager
import com.boardgamegeek.R
import com.boardgamegeek.extensions.TAG
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditCollectionWishlistCommentDialogFragment : EditCollectionTextDialogFragment() {
    override val titleResId = R.string.wishlist_comment

    override fun updateText(viewModel: GameCollectionItemViewModel, text: String) {
        viewModel.updateWishlistComment(text, originalText)
    }

    companion object {
        fun show(fragmentManager: FragmentManager, text: String) {
            EditCollectionWishlistCommentDialogFragment().apply {
                arguments = createBundle(text)
            }.show(fragmentManager, EditCollectionWishlistCommentDialogFragment.TAG)
        }
    }
}
