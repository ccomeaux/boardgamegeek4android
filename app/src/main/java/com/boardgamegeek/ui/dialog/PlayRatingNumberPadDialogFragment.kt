package com.boardgamegeek.ui.dialog

import android.content.Context
import com.boardgamegeek.R

class PlayRatingNumberPadDialogFragment : NumberPadDialogFragment() {
    private var listener: Listener? = null

    interface Listener {
        fun onNumberPadDone(output: Double, requestCode: Int)
    }

    override fun onAttach(context: Context) {
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
        fun newInstance(requestCode: Int,
                        initialValue: String,
                        colorDescription: String? = null,
                        subtitle: String? = null
        ): PlayRatingNumberPadDialogFragment {
            return PlayRatingNumberPadDialogFragment().apply {
                arguments = createBundle(requestCode, R.string.rating, initialValue, colorDescription, subtitle, 1.0, 10.0, 6)
            }
        }
    }
}
