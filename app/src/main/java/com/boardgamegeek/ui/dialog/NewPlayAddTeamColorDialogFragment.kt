package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import kotlinx.android.synthetic.main.dialog_edit_text.*
import org.jetbrains.anko.support.v4.withArguments

class NewPlayAddTeamColorDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<NewPlayViewModel>()

    override val titleResId = R.string.title_add_team_color

    override val hintResId = R.string.team_color

    override fun onPositiveButton() {
        val playerIndex = arguments?.getInt(PLAYER_INDEX, -1) ?: -1
        if (playerIndex > -1) {
            val text = editText?.text?.toString() ?: ""
            viewModel.addColorToPlayer(playerIndex, text)
        }
    }

    companion object {
        private const val PLAYER_INDEX = "PLAYER_INDEX"

        fun newInstance(playerIndex: Int): NewPlayAddTeamColorDialogFragment {
            return NewPlayAddTeamColorDialogFragment().withArguments(
                    PLAYER_INDEX to playerIndex
            )
        }
    }
}
