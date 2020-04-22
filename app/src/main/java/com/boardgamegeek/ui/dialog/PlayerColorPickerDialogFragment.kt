package com.boardgamegeek.ui.dialog

import android.util.Pair
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.R
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.PlayerColorsViewModel
import com.boardgamegeek.util.ColorUtils
import com.boardgamegeek.util.fabric.PlayerColorsManipulationEvent
import java.util.*

class PlayerColorPickerDialogFragment : ColorPickerDialogFragment() {
    private val viewModel by activityViewModels<PlayerColorsViewModel>()

    override fun onColorClicked(item: Pair<String, Int>?, requestCode: Int) {
        item?.let {
            PlayerColorsManipulationEvent.log("Add", it.first)
            viewModel.add(it.first)
        }
    }

    companion object {
        fun launch(activity: FragmentActivity, hiddenColors: ArrayList<String>) {
            val dialogFragment = PlayerColorPickerDialogFragment().apply {
                arguments = createBundle(
                        R.string.title_add_color,
                        ColorUtils.getColorList(),
                        null,
                        null,
                        null,
                        4,
                        0,
                        hiddenColors)

            }
            activity.showAndSurvive(dialogFragment)
        }
    }
}
