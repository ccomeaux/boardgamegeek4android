package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.ui.viewmodel.CollectionDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailsRatingNumberPadDialogFragment : NumberPadDialogFragment() {
    private val viewModel by activityViewModels<CollectionDetailsViewModel>()
    private var internalId: Long = INVALID_ID.toLong()

    override fun fetchArguments() {
        internalId = arguments?.getLong(INTERNAL_ID) ?: INVALID_ID.toLong()
    }

    override fun done(output: Double, requestCode: Int, requestKey: String) {
        viewModel.updateRating(internalId, output)
    }

    companion object {
        private const val INTERNAL_ID = "INTERNAL_ID"

        fun newInstance(internalId: Long, gameName: String) = CollectionDetailsRatingNumberPadDialogFragment().apply {
            arguments = createBundle(0, R.string.rating, "", null, gameName, 1.0, 10.0, 6).apply {
                putLong(INTERNAL_ID, internalId)
            }
        }
    }
}
