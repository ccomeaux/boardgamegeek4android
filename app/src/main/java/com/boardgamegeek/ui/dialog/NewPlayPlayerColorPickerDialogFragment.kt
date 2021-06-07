package com.boardgamegeek.ui.dialog

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.boardgamegeek.extensions.showAndSurvive
import com.boardgamegeek.ui.viewmodel.NewPlayViewModel
import com.boardgamegeek.util.ColorUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent

class NewPlayPlayerColorPickerDialogFragment : ColorPickerDialogFragment() {
    private val viewModel by activityViewModels<NewPlayViewModel>()

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

    override fun onAddClicked(requestCode: Int) {
        NewPlayAddTeamColorDialogFragment.newInstance(requestCode).show(parentFragmentManager, "")
        dismiss()
    }

    companion object {
        fun launch(activity: FragmentActivity, playerDescription: String, featuredColors: List<String>, selectedColor: String?, disabledColors: List<String>, playerIndex: Int) {
            val df = NewPlayPlayerColorPickerDialogFragment().apply {
                arguments = createBundle(
                        title = playerDescription,
                        ColorUtils.colorList,
                        ArrayList(featuredColors),
                        selectedColor,
                        ArrayList(disabledColors),
                        requestCode = playerIndex,
                        showAddButton = true)
            }
            activity.showAndSurvive(df)
        }
    }
}
