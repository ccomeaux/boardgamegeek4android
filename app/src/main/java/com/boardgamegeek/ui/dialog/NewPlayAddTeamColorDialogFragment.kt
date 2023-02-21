package com.boardgamegeek.ui.dialog

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewPlayAddTeamColorDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<NewPlayViewModel>()

    override val titleResId = R.string.title_add_team_color

    override val hintResId = R.string.team_color

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val maxLength = 32
            binding.editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
            binding.editTextContainer.counterMaxLength = maxLength
            binding.editTextContainer.isCounterEnabled = true
        }
    }

    override fun onPositiveButton() {
        val playerIndex = arguments?.getInt(PLAYER_INDEX, -1) ?: -1
        if (playerIndex > -1) {
            val text = binding.editText.text?.toString().orEmpty()
            viewModel.addColorToPlayer(playerIndex, text)
        }
    }

    companion object {
        private const val PLAYER_INDEX = "PLAYER_INDEX"

        fun newInstance(playerIndex: Int) = NewPlayAddTeamColorDialogFragment().apply {
            arguments = bundleOf(PLAYER_INDEX to playerIndex)
        }
    }
}
