package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.LogPlayViewModel
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.util.ColorUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class LogPlayPlayerColorPickerDialogFragment : ColorPickerDialogFragment() {
    private val viewModel by activityViewModels<LogPlayViewModel>()

    override fun onColorClicked(item: Pair<String, Int>?, requestCode: Int) {
        item?.let {
            FirebaseAnalytics.getInstance(requireContext()).logEvent("DataManipulation") {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "PlayerColors")
                param("Action", "Add")
                param("Color", it.first)
            }
            viewModel.addColorToPlayer(requestCode, it.first)
        }
    }

    companion object {
        fun launch(activity: FragmentActivity, playerDescription: String, featuredColors: List<String>, selectedColor: String?, disabledColors: List<String>, playerIndex: Int) {
            val df = LogPlayPlayerColorPickerDialogFragment().apply {
                arguments = createBundle(
                        title = playerDescription,
                        ColorUtils.colorList,
                        ArrayList(featuredColors),
                        selectedColor,
                        ArrayList(disabledColors),
                        requestCode = playerIndex)
            }
            activity.showAndSurvive(df)
        }
    }
}
