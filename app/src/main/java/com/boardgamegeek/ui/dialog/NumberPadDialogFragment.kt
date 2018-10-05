package com.boardgamegeek.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asColorRgb
import com.boardgamegeek.extensions.isColorDark
import com.boardgamegeek.extensions.setTextOrHide
import com.boardgamegeek.util.PreferencesUtils
import kotlinx.android.synthetic.main.dialog_number_pad.*
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

class NumberPadDialogFragment : DialogFragment() {
    private var listener: Listener? = null
    private var minValue = DEFAULT_MIN_VALUE
    private var maxValue = DEFAULT_MAX_VALUE
    private var maxMantissa = DEFAULT_MAX_MANTISSA

    interface Listener {
        fun onNumberPadDone(output: String, requestCode: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }

    override fun onResume() {
        super.onResume()
        val window = dialog.window
        if (window != null) {
            val dm = resources.displayMetrics
            val width = Math.min(
                    act.resources.getDimensionPixelSize(R.dimen.dialog_width),
                    dm.widthPixels * 3 / 4)
            val height = window.attributes.height
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_number_pad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        minValue = arguments?.getDouble(KEY_MIN_VALUE) ?: DEFAULT_MIN_VALUE
        maxValue = arguments?.getDouble(KEY_MAX_VALUE) ?: DEFAULT_MAX_VALUE
        maxMantissa = arguments?.getInt(KEY_MAX_MANTISSA) ?: DEFAULT_MAX_MANTISSA

        if (arguments?.containsKey(KEY_TITLE) == true) {
            titleView.text = arguments?.getString(KEY_TITLE)
        }
        subtitleView.setTextOrHide(arguments?.getString(KEY_SUBTITLE))

        if (arguments?.containsKey(KEY_OUTPUT) == true) {
            outputView.text = arguments?.getString(KEY_OUTPUT)
            enableDelete()
        }

        if (arguments?.containsKey(KEY_COLOR) == true) {
            val color = arguments?.getInt(KEY_COLOR) ?: Color.TRANSPARENT
            headerView.setBackgroundColor(color)
            if (color != Color.TRANSPARENT && color.isColorDark()) {
                titleView.setTextColor(Color.WHITE)
                subtitleView.setTextColor(Color.WHITE)
            } else {
                titleView.setTextColor(Color.BLACK)
                subtitleView.setTextColor(Color.BLACK)
            }
        }

        deleteView.setOnClickListener {
            val text = outputView.text
            if (text.isNotEmpty()) {
                val output = text.subSequence(0, text.length - 1).toString()
                maybeUpdateOutput(output, view)
            }
        }
        deleteView.setOnLongClickListener {
            outputView.text = ""
            enableDelete()
            true
        }

        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: DEFAULT_REQUEST_CODE
        doneView.setOnClickListener {
            listener?.onNumberPadDone(outputView.text.toString(), requestCode)
            dismiss()
        }

        plusMinusView.setOnClickListener {
            val output = outputView.text.toString()
            val signedOutput = if (output.isNotEmpty() && output[0] == '-') {
                output.substring(1)
            } else {
                "-$output"
            }
            maybeUpdateOutput(signedOutput, it)
        }

        numberPadView.childrenRecursiveSequence().filterIsInstance<TextView>().forEach {
            it.setOnClickListener { textView ->
                val output = outputView.text.toString() + (textView as TextView).text
                maybeUpdateOutput(output, it)
            }
        }
    }

    private fun maybeUpdateOutput(output: String, view: View) {
        if (isWithinLength(output) && isWithinRange(output)) {
            maybeBuzz(view)
            outputView.text = output
            enableDelete()
        }
    }

    private fun maybeBuzz(v: View) {
        if (PreferencesUtils.getHapticFeedback(ctx)) {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun isWithinLength(text: String): Boolean {
        return text.isEmpty() || text.length <= DEFAULT_MAX_MANTISSA && getMantissaLength(text) <= maxMantissa
    }

    private fun getMantissaLength(text: String): Int {
        if (!text.contains(".")) {
            return 0
        }
        val parts = text.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (parts.size > 1) {
            parts[1].length
        } else 0
    }

    private fun isWithinRange(text: String): Boolean {
        if (text.isEmpty() || "." == text || "-." == text) {
            return true
        }
        if (hasTwoDecimalPoints(text)) {
            return false
        }
        val value = parseDouble(text)
        return value in minValue..maxValue
    }

    private fun hasTwoDecimalPoints(text: String?): Boolean {
        if (text == null) return false
        val decimalIndex = text.indexOf('.')
        return decimalIndex >= 0 && text.indexOf('.', decimalIndex + 1) >= 0
    }

    private fun parseDouble(text: String): Double {
        return when {
            text.isEmpty() || "." == text || "-" == text || "-." == text -> 0.0
            text.endsWith(".") -> "${text}0".toDouble()
            text.startsWith(".") -> "0$text".toDouble()
            text.startsWith("-.") -> ("-0" + text.substring(1)).toDouble()
            else -> text.toDouble()
        }
    }

    private fun enableDelete() {
        deleteView.isEnabled = outputView.length() > 0
    }

    companion object {
        private const val KEY_TITLE = "TITLE"
        private const val KEY_SUBTITLE = "SUBTITLE"
        private const val KEY_OUTPUT = "OUTPUT"
        private const val KEY_COLOR = "COLOR"
        private const val KEY_MIN_VALUE = "MIN_VALUE"
        private const val KEY_MAX_VALUE = "MAX_VALUE"
        private const val KEY_MAX_MANTISSA = "MAX_MANTISSA"
        private const val KEY_REQUEST_CODE = "REQUEST_CODE"
        private val DEFAULT_MIN_VALUE = -Double.MAX_VALUE
        private val DEFAULT_MAX_VALUE = Double.MAX_VALUE
        private const val DEFAULT_MAX_MANTISSA = 10
        private const val DEFAULT_REQUEST_CODE = 0

        @JvmStatic
        @JvmOverloads
        fun newInstance(
                title: String, output: String,
                colorDescription: String?,
                subtitle: String?,
                requestCode: Int = DEFAULT_REQUEST_CODE,
                minValue: Double = DEFAULT_MIN_VALUE,
                maxValue: Double = DEFAULT_MAX_VALUE,
                maxMantissa: Int = DEFAULT_MAX_MANTISSA
        ): NumberPadDialogFragment {
            return NumberPadDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    if (!subtitle.isNullOrBlank()) {
                        putString(KEY_SUBTITLE, subtitle)
                    }
                    if (output.toDoubleOrNull() != null) {
                        putString(KEY_OUTPUT, output)
                    }
                    val color = colorDescription.asColorRgb()
                    if (color != Color.TRANSPARENT) {
                        putInt(KEY_COLOR, color)
                    }
                    putDouble(KEY_MIN_VALUE, minValue)
                    putDouble(KEY_MAX_VALUE, maxValue)
                    putInt(KEY_MAX_MANTISSA, maxMantissa)
                    putInt(KEY_REQUEST_CODE, requestCode)
                }
            }
        }
    }
}
