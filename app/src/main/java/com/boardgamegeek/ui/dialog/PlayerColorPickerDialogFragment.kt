package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.BggColors
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.PlayerColorsViewModel

class PlayerColorPickerDialogFragment : ColorPickerDialogFragment() {
    private val viewModel by activityViewModels<PlayerColorsViewModel>()

    override fun onColorClicked(item: Pair<String, Int>?, requestCode: Int) {
        item?.let { viewModel.add(it.first) }
    }

    companion object {
        fun launch(activity: FragmentActivity, hiddenColors: ArrayList<String>) {
            val dialogFragment = PlayerColorPickerDialogFragment().apply {
                arguments = createBundle(
                    R.string.title_add_color,
                    BggColors.colorList,
                    hiddenColors = hiddenColors,
                )
            }
            activity.showAndSurvive(dialogFragment)
        }
    }
}
