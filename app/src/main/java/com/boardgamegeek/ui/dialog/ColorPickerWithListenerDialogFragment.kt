package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.util.ColorUtils
import java.util.*

class ColorPickerWithListenerDialogFragment : ColorPickerDialogFragment() {
    private var listener: Listener? = null

    interface Listener {
        fun onColorSelected(description: String, color: Int, requestCode: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onColorClicked(item: Pair<String, Int>?, requestCode: Int) {
        if (item != null) listener?.onColorSelected(item.first, item.second, requestCode)
    }

    companion object {
        /**
         * Constructor
         *
         * @param featuredColors subset of the list of colors that should be featured above the rest
         * @param selectedColor  selected color
         * @param disabledColors colors that should be displayed as disabled (but still selectable
         * @return new ColorPickerDialog
         */
        fun newInstance(featuredColors: ArrayList<String>?,
                        selectedColor: String?,
                        disabledColors: ArrayList<String>?,
                        requestCode: Int = 0): ColorPickerWithListenerDialogFragment {
            return ColorPickerWithListenerDialogFragment().apply {
                arguments = createBundle(
                        0,
                        ColorUtils.colorList,
                        featuredColors,
                        selectedColor,
                        disabledColors,
                        requestCode = requestCode)

            }
        }
    }
}
