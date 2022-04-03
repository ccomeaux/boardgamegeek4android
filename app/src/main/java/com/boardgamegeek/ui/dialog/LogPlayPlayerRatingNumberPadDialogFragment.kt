package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.LogPlayViewModel

class LogPlayPlayerRatingNumberPadDialogFragment : NumberPadDialogFragment() {
    private val viewModel by activityViewModels<LogPlayViewModel>()

    override fun done(output: Double, requestCode: Int, requestKey: String) {
        viewModel.addRatingToPlayer(requestCode, output)
    }

    companion object {
        fun newInstance(
            requestCode: Int,
            initialValue: String,
            colorDescription: String? = null,
            subtitle: String? = null,
        ) = LogPlayPlayerRatingNumberPadDialogFragment().apply {
            arguments = createBundle(requestCode, R.string.rating, initialValue, colorDescription, subtitle, 1.0, 10.0, 6)
        }
    }
}
