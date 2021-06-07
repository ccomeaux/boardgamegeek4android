package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.LogPlayViewModel

class LogPlayPlayerScoreNumberPadDialogFragment : NumberPadDialogFragment() {
    private val viewModel by activityViewModels<LogPlayViewModel>()

    override fun done(output: Double, requestCode: Int, requestKey: String) {
        viewModel.addScoreToPlayer(requestCode, output)
    }

    companion object {
        fun newInstance(
                requestCode: Int,
                initialValue: String,
                colorDescription: String? = null,
                subtitle: String? = null
        ): NumberPadDialogFragment {
            return LogPlayPlayerScoreNumberPadDialogFragment().apply {
                arguments = createBundle(requestCode, R.string.score, initialValue, colorDescription, subtitle)
            }
        }
    }
}
