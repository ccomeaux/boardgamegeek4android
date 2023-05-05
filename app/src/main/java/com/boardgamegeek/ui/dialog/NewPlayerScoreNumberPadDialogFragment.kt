package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewPlayerScoreNumberPadDialogFragment : NumberPadDialogFragment() {
    private val viewModel by activityViewModels<NewPlayViewModel>()

    override fun done(output: Double, requestCode: Int, requestKey: String) {
        viewModel.addScoreToPlayer(requestKey, output)
    }

    companion object {
        fun newInstance(
            playerId: String,
            initialValue: String,
            colorDescription: String? = null,
            subtitle: String? = null,
        ) = NewPlayerScoreNumberPadDialogFragment().apply {
            arguments = createBundle(0, R.string.score, initialValue, colorDescription, subtitle, requestKey = playerId)
        }
    }
}
