package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R

class ScoreNumberPadDialogFragment : NumberPadDialogFragment() {
    private var listener: Listener? = null

    interface Listener {
        fun onNumberPadDone(output: Double, requestCode: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun done(output: Double, requestCode: Int) {
        listener?.onNumberPadDone(output, requestCode)
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun newInstance(
                requestCode: Int,
                initialValue: String,
                colorDescription: String? = null,
                subtitle: String? = null,
                minValue: Double = DEFAULT_MIN_VALUE,
                maxValue: Double = DEFAULT_MAX_VALUE,
                maxMantissa: Int = DEFAULT_MAX_MANTISSA
        ): NumberPadDialogFragment {
            return ScoreNumberPadDialogFragment().apply {
                arguments = createBundle(requestCode, R.string.score, initialValue, colorDescription, subtitle, minValue, maxValue, maxMantissa)
            }
        }
    }
}
