package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.GameCollectionItemViewModel

class CollectionRatingNumberPadDialogFragment : NumberPadDialogFragment() {
    private val viewModel by activityViewModels<GameCollectionItemViewModel>()

    override fun done(output: Double, requestCode: Int) {
        viewModel.updateRating(output)
    }

    companion object {
        fun newInstance(initialValue: String): CollectionRatingNumberPadDialogFragment {
            return CollectionRatingNumberPadDialogFragment().apply {
                arguments = createBundle(0, R.string.rating, initialValue, null, null, 1.0, 10.0, 6)
            }
        }
    }
}
