package com.boardgamegeek.ui.dialog

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogNumberPadBinding
import com.boardgamegeek.extensions.*
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import kotlin.math.min

abstract class NumberPadDialogFragment : DialogFragment() {
    private var _binding: DialogNumberPadBinding? = null
    private val binding get() = _binding!!
    private var minValue = DEFAULT_MIN_VALUE
    private var maxValue = DEFAULT_MAX_VALUE
    private var maxMantissa = DEFAULT_MAX_MANTISSA
    private val decimal = DecimalFormatSymbols.getInstance().decimalSeparator
    private var useHapticFeedback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    private val preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_HAPTIC_FEEDBACK) {
                useHapticFeedback = sharedPreferences[KEY_HAPTIC_FEEDBACK, true] == true
            }
        }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let { window ->
            val width = min(
                requireActivity().resources.getDimensionPixelSize(R.dimen.dialog_width),
                resources.displayMetrics.widthPixels * 3 / 4
            )
            val height = window.attributes.height
            window.setLayout(width, height)
        }
        useHapticFeedback = requireContext().preferences()[KEY_HAPTIC_FEEDBACK, true] == true
        requireContext().preferences().registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onPause() {
        super.onPause()
        requireContext().preferences().unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogNumberPadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.decimalSeparator.text = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

        minValue = arguments?.getDouble(KEY_MIN_VALUE) ?: DEFAULT_MIN_VALUE
        maxValue = arguments?.getDouble(KEY_MAX_VALUE) ?: DEFAULT_MAX_VALUE
        maxMantissa = arguments?.getInt(KEY_MAX_MANTISSA) ?: DEFAULT_MAX_MANTISSA
        fetchArguments()

        binding.plusMinusView.visibility = if (minValue >= 0.0) View.GONE else View.VISIBLE

        val titleResId = arguments?.getInt(KEY_TITLE) ?: 0
        if (titleResId != 0) binding.titleView.setText(titleResId)
        binding.subtitleView.setTextOrHide(arguments?.getString(KEY_SUBTITLE))

        if (arguments?.containsKey(KEY_INITIAL_VALUE) == true) {
            binding.outputView.text = arguments?.getString(KEY_INITIAL_VALUE)
            enableDelete()
        }

        if (arguments?.containsKey(KEY_COLOR) == true) {
            val color = arguments?.getInt(KEY_COLOR) ?: Color.TRANSPARENT
            binding.headerView.setBackgroundColor(color)
            if (color != Color.TRANSPARENT && color.isColorDark()) {
                binding.titleView.setTextColor(Color.WHITE)
                binding.subtitleView.setTextColor(Color.WHITE)
            } else {
                binding.titleView.setTextColor(Color.BLACK)
                binding.subtitleView.setTextColor(Color.BLACK)
            }
        }

        binding.deleteView.setOnClickListener {
            val text = binding.outputView.text
            if (text.isNotEmpty()) {
                val output = text.subSequence(0, text.length - 1).toString()
                maybeUpdateOutput(output, view)
            }
        }
        binding.deleteView.setOnLongClickListener {
            binding.outputView.clearText()
            enableDelete()
            true
        }

        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: DEFAULT_REQUEST_CODE
        val requestKey = arguments?.getString(KEY_REQUEST_KEY).orEmpty()
        binding.doneView.setOnClickListener {
            done(parseDouble(binding.outputView.text.toString()), requestCode, requestKey)
            dismiss()
        }

        binding.plusMinusView.setOnClickListener {
            val output = binding.outputView.text.toString()
            val signedOutput = if (output.isNotEmpty() && output[0] == '-') {
                output.substring(1)
            } else {
                "-$output"
            }
            maybeUpdateOutput(signedOutput, it)
        }

        binding.numberPadView.childrenRecursiveSequence().filterIsInstance<TextView>().forEach {
            it.setOnClickListener { textView ->
                val output = binding.outputView.text.toString() + (textView as TextView).text
                maybeUpdateOutput(output, it)
            }
        }
    }

    open fun fetchArguments() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun done(output: Double, requestCode: Int, requestKey: String)

    private fun maybeUpdateOutput(output: String, view: View) {
        if (isWithinLength(output) && isWithinRange(output)) {
            if (useHapticFeedback)
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            binding.outputView.text = output
            enableDelete()
        }
    }

    private fun isWithinLength(text: String): Boolean {
        return text.isEmpty() || text.length <= DEFAULT_MAX_MANTISSA && getMantissaLength(text) <= maxMantissa
    }

    private fun getMantissaLength(text: String): Int {
        if (!text.contains(decimal)) {
            return 0
        }
        val parts = text.trim().split(decimal).toTypedArray()
        return if (parts.size > 1) {
            parts[1].length
        } else 0
    }

    private fun isWithinRange(text: String): Boolean {
        if (text.isEmpty() || decimal.toString() == text || "-$decimal" == text) {
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
        val decimalIndex = text.indexOf(decimal)
        return decimalIndex >= 0 && text.indexOf(decimal, decimalIndex + 1) >= 0
    }

    private fun parseDouble(text: String): Double {
        val parsableText = when {
            text.isEmpty() || decimal.toString() == text || "-" == text || "-$decimal" == text -> ""
            text.endsWith(decimal) -> "${text}0"
            text.startsWith(decimal) -> "0$text"
            text.startsWith("-$decimal") -> "-0${text.substring(1)}"
            else -> text
        }
        return try {
            NumberFormat.getNumberInstance().parse(parsableText)?.toDouble() ?: 0.0
        } catch (ex: ParseException) {
            0.0
        }
    }

    private fun enableDelete() {
        binding.deleteView.isEnabled = binding.outputView.length() > 0
    }

    companion object {
        private const val KEY_TITLE = "TITLE"
        private const val KEY_SUBTITLE = "SUBTITLE"
        private const val KEY_INITIAL_VALUE = "INITIAL_VALUE"
        private const val KEY_COLOR = "COLOR"
        private const val KEY_MIN_VALUE = "MIN_VALUE"
        private const val KEY_MAX_VALUE = "MAX_VALUE"
        private const val KEY_MAX_MANTISSA = "MAX_MANTISSA"
        private const val KEY_REQUEST_CODE = "REQUEST_CODE"
        private const val KEY_REQUEST_KEY = "KEY"
        const val DEFAULT_MIN_VALUE = -Double.MAX_VALUE
        const val DEFAULT_MAX_VALUE = Double.MAX_VALUE
        const val DEFAULT_MAX_MANTISSA = 10
        const val DEFAULT_REQUEST_CODE = 0

        fun createBundle(
            requestCode: Int,
            @StringRes titleResId: Int,
            initialValue: String,
            colorDescription: String?,
            subtitle: String?,
            minValue: Double = DEFAULT_MIN_VALUE,
            maxValue: Double = DEFAULT_MAX_VALUE,
            maxMantissa: Int = DEFAULT_MAX_MANTISSA,
            requestKey: String = "",
        ): Bundle {
            return Bundle().apply {
                putInt(KEY_TITLE, titleResId)
                if (!subtitle.isNullOrBlank()) {
                    putString(KEY_SUBTITLE, subtitle)
                }
                val initialNumber = try {
                    NumberFormat.getInstance().parse(initialValue)?.toDouble()
                } catch (e: ParseException) {
                    null
                }
                if (initialNumber != null) {
                    putString(KEY_INITIAL_VALUE, initialValue)
                }
                val color = colorDescription.asColorRgb()
                if (color != Color.TRANSPARENT) {
                    putInt(KEY_COLOR, color)
                }
                putDouble(KEY_MIN_VALUE, minValue)
                putDouble(KEY_MAX_VALUE, maxValue)
                putInt(KEY_MAX_MANTISSA, maxMantissa)
                putInt(KEY_REQUEST_CODE, requestCode)
                putString(KEY_REQUEST_KEY, requestKey)
            }
        }
    }
}
