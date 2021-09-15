package com.boardgamegeek.ui.dialog

import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.ui.viewmodel.GameColorsViewModel
import kotlinx.android.synthetic.main.dialog_edit_text.*

class AddColorToGameDialogFragment : AbstractEditTextDialogFragment() {
    private val viewModel by activityViewModels<GameColorsViewModel>()

    override val titleResId = R.string.title_add_color

    override val hintResId = R.string.color_name

    override fun onPositiveButton() {
        val text = editText?.text?.toString()
        viewModel.addColor(text)
    }
}
